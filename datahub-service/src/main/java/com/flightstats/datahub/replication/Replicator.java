package com.flightstats.datahub.replication;

import com.google.inject.Inject;
import com.google.inject.Provider;
import org.apache.curator.framework.CuratorFramework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Replication is moving from one Hub into another Hub
 * in Replication, we will presume we are moving forward in time, starting with (nearly) the oldest Item
 * <p/>
 * Secnario:
 * Producers are inserting Items into a Hub channel
 * The Hub is setup to Replicate a channel from a Hub
 * Replication starts at nearly the oldest Item, and gradually progresses forward to the current item
 * Replication stays up to date, with some minimal amount of lag
 */
public class Replicator {
    public static final String REPLICATOR_WATCHER = "/replicator/watcher";
    private final static Logger logger = LoggerFactory.getLogger(Replicator.class);

    private final ChannelUtils channelUtils;
    private final Provider<ChannelReplicator> replicatorProvider;
    private final ReplicationService replicationService;
    private final CuratorFramework curator;
    private final ScheduledExecutorService executorService;
    private final List<SourceReplicator> replicators = new ArrayList<>();

    @Inject
    public Replicator(ChannelUtils channelUtils, Provider<ChannelReplicator> replicatorProvider,
                      ReplicationService replicationService, CuratorFramework curator) {
        this.channelUtils = channelUtils;
        this.replicatorProvider = replicatorProvider;
        this.replicationService = replicationService;
        this.curator = curator;
        executorService = Executors.newScheduledThreadPool(10);
    }

    public void startThreads() {
        //todo - gfm - 1/28/14 - figure out watcher semantics
        try {
            curator.getData().watched().inBackground().forPath(REPLICATOR_WATCHER);
        } catch (Exception e) {
            logger.warn("unable to start watcher", e);
        }
        Collection<ReplicationConfig> configs = replicationService.getConfigs();
        for (ReplicationConfig config : configs) {

            logger.info("starting repication of " + config.getDomain());
            SourceReplicator replicator = new SourceReplicator(config);
            replicators.add(replicator);
            executorService.scheduleWithFixedDelay(replicator, 0, 1, TimeUnit.MINUTES);
        }
    }

    public List<SourceReplicator> getReplicators() {
        return Collections.unmodifiableList(replicators);
    }

    public class SourceReplicator implements Runnable {
        private final Set<String> migratingChannels = new HashSet<>();
        private final ReplicationConfig config;

        public Set<String> getSourceChannelUrls() {
            return Collections.unmodifiableSet(migratingChannels);
        }

        private SourceReplicator(ReplicationConfig config) {
            this.config = config;
        }

        @Override
        public void run() {
            String sourceUrl = "http://" + config.getDomain() +"/channel/";
            Set <String> rawChannels = channelUtils.getChannels(sourceUrl);
            if (rawChannels.isEmpty()) {
                logger.warn("did not find any channels to replicate at " + sourceUrl);
                return;
            }
            //todo - gfm - 1/28/14 - filter inclusion & excelusion sets
            Set<String> filtered = new HashSet<>();
            for (String channel : rawChannels) {
                if (channel.startsWith(sourceUrl)) {
                    filtered.add(channel);
                }
            }
            //todo - gfm - 1/23/14 - does this need to support channel removal?
            filtered.removeAll(migratingChannels);
            for (String channelUrl : filtered) {
                logger.info("found new channel " + channelUrl);
                ChannelReplicator channelReplicator = replicatorProvider.get();
                channelReplicator.setChannelUrl(channelUrl);
                executorService.scheduleWithFixedDelay(channelReplicator, 0, 15, TimeUnit.SECONDS);
                migratingChannels.add(channelUrl);
            }
        }
    }

}

package com.flightstats.datahub.replication;

import com.flightstats.datahub.dao.ChannelService;
import com.google.common.base.Splitter;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.name.Named;
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
 * The Hub is setup to Replicate a channel from DataHub
 * Replication starts at nearly the oldest Item, and gradually progresses forward to the current item
 * Replication stays up to date, with some minimal amount of lag
 */
public class Replicator {
    private final static Logger logger = LoggerFactory.getLogger(Replicator.class);

    private final ChannelUtils channelUtils;
    private final String sourceUrls;
    private final ChannelService channelService;
    private final CuratorFramework curator;
    private final SequenceIteratorFactory sequenceIteratorFactory;
    private final ScheduledExecutorService executorService;
    private final List<SourceReplicator> replicators = new ArrayList<>();

    @Inject
    public Replicator(ChannelUtils channelUtils, @Named("migration.source.urls") String sourceUrls,
                      ChannelService channelService, CuratorFramework curator, SequenceIteratorFactory sequenceIteratorFactory) {
        this.channelUtils = channelUtils;
        this.sourceUrls = sourceUrls;
        this.channelService = channelService;
        this.curator = curator;
        this.sequenceIteratorFactory = sequenceIteratorFactory;
        executorService = Executors.newScheduledThreadPool(10);
    }

    public void startThreads() {
        if (sourceUrls.isEmpty()) {
            logger.info("nothing to migrate");
            return;
        }
        Iterable<String> iterable = Splitter.on("|").omitEmptyStrings().trimResults().split(sourceUrls);
        Set<String> targetSet = Sets.newHashSet(iterable);
        for (String target : targetSet) {
            logger.info("starting migration of " + target);
            SourceReplicator replicator = new SourceReplicator(target);
            replicators.add(replicator);
            executorService.scheduleWithFixedDelay(replicator, 0, 1, TimeUnit.MINUTES);
        }
    }

    public List<SourceReplicator> getReplicators() {
        return Collections.unmodifiableList(replicators);
    }

    public class SourceReplicator implements Runnable {
        private final String sourceUrl;
        private final Set<String> migratingChannels = new HashSet<>();

        public Set<String> getSourceChannelUrls() {
            return Collections.unmodifiableSet(migratingChannels);
        }

        private SourceReplicator(String sourceUrl) {
            this.sourceUrl = sourceUrl;
        }

        @Override
        public void run() {
            Set<String> rawChannels = channelUtils.getChannels(sourceUrl);
            if (rawChannels.isEmpty()) {
                logger.warn("did not find any channels to migrate at " + sourceUrl);
                return;
            }
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
                ChannelReplicator migrator = new ChannelReplicator(channelService, channelUrl, channelUtils, curator, sequenceIteratorFactory);
                executorService.scheduleWithFixedDelay(migrator, 0, 15, TimeUnit.SECONDS);
                migratingChannels.add(channelUrl);
            }
        }
    }

}

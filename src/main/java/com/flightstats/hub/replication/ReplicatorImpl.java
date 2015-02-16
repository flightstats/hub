package com.flightstats.hub.replication;

import com.flightstats.hub.app.HubServices;
import com.flightstats.hub.cluster.WatchManager;
import com.flightstats.hub.cluster.Watcher;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.model.ChannelConfiguration;
import com.flightstats.hub.util.HubUtils;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Inject;
import com.google.inject.Provider;
import org.apache.curator.framework.api.CuratorEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Replication is moving from one Hub into another Hub
 * in Replication, we will presume we are moving forward in time, starting with configurable item age.
 * <p>
 * Secnario:
 * Producers are inserting Items into a Hub channel
 * The Hub is setup to Replicate a channel from a Hub
 * Replication starts at nearly the oldest Item, and gradually progresses forward to the current item
 * Replication stays up to date, with some minimal amount of lag
 */
public class ReplicatorImpl implements Replicator {
    private static final String REPLICATOR_WATCHER_PATH = "/replicator/watcher";
    private final static Logger logger = LoggerFactory.getLogger(ReplicatorImpl.class);

    private final ChannelService channelService;
    private final HubUtils hubUtils;
    private final Provider<V1ChannelReplicator> v1ReplicatorProvider;
    private final WatchManager watchManager;
    private final Map<String, ChannelReplicator> replicatorMap = new HashMap<>();
    private final AtomicBoolean stopped = new AtomicBoolean();
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    @Inject
    public ReplicatorImpl(ChannelService channelService, HubUtils hubUtils,
                          Provider<V1ChannelReplicator> v1ReplicatorProvider, WatchManager watchManager) {
        this.channelService = channelService;
        this.hubUtils = hubUtils;
        this.v1ReplicatorProvider = v1ReplicatorProvider;
        this.watchManager = watchManager;
        HubServices.registerPreStop(new ReplicatorService());
    }

    private class ReplicatorService extends AbstractIdleService {

        @Override
        protected void startUp() throws Exception {
            startReplicator();
        }

        @Override
        protected void shutDown() throws Exception {
            stopped.set(true);
            exit();
        }

    }

    public void startReplicator() {
        logger.info("starting");
        ReplicatorImpl replicator = this;
        watchManager.register(new Watcher() {
            @Override
            public void callback(CuratorEvent event) {
                executor.submit(replicator::replicateChannels);
            }

            @Override
            public String getPath() {
                return REPLICATOR_WATCHER_PATH;
            }
        });

        executor.submit(replicator::replicateChannels);
    }

    private synchronized void replicateChannels() {
        if (stopped.get()) {
            logger.info("replication stopped");
            return;
        }
        logger.info("replicating channels");
        Set<String> replicators = new HashSet<>();
        Iterable<ChannelConfiguration> replicatedChannels = channelService.getChannels(REPLICATED);
        for (ChannelConfiguration channel : replicatedChannels) {
            if (replicatorMap.containsKey(channel.getName())) {
                ChannelReplicator replicator = replicatorMap.get(channel.getName());
                if (!replicator.getChannel().getReplicationSource().equals(channel.getReplicationSource())) {
                    logger.info("changing replication source from {} to {}",
                            replicator.getChannel().getReplicationSource(), channel.getReplicationSource());
                    replicator.stop();
                    startReplication(channel);
                }
            } else {
                startReplication(channel);
            }
            replicators.add(channel.getName());
        }
        Set<String> toStop = new HashSet<>(replicatorMap.keySet());
        toStop.removeAll(replicators);
        logger.info("stopping replicators {}", toStop);
        for (String nameToStop : toStop) {
            logger.info("stopping {}", nameToStop);
            ChannelReplicator replicator = replicatorMap.remove(nameToStop);
            replicator.stop();
        }
    }

    private void exit() {
        logger.info("exiting all replication " + replicatorMap.keySet());
        Collection<ChannelReplicator> replicators = replicatorMap.values();
        for (ChannelReplicator replicator : replicators) {
            replicator.exit();
        }
        logger.info("exited all replication " + replicatorMap.keySet());
    }

    private void startReplication(ChannelConfiguration channel) {
        logger.info("starting replication of " + channel);
        HubUtils.Version version = hubUtils.getHubVersion(channel.getReplicationSource());
        if (version.equals(HubUtils.Version.V2)) {
            startV2Replication(channel);
        } else if (version.equals(HubUtils.Version.V1)) {
            startV1Replication(channel);
        }
    }

    private void startV2Replication(ChannelConfiguration channel) {
        logger.debug("starting v2 replication of " + channel);
        try {
            V2ChannelReplicator v2ChannelReplicator = new V2ChannelReplicator(channel, hubUtils);
            v2ChannelReplicator.start();
            replicatorMap.put(channel.getName(), v2ChannelReplicator);
        } catch (Exception e) {
            logger.warn("unable to start v2 replication " + channel, e);
        }
    }

    private void startV1Replication(ChannelConfiguration channel) {
        logger.debug("starting v1 replication of " + channel);
        try {
            V1ChannelReplicator v1ChannelReplicator = v1ReplicatorProvider.get();
            v1ChannelReplicator.setChannel(channel);
            if (v1ChannelReplicator.tryLeadership()) {
                replicatorMap.put(channel.getName(), v1ChannelReplicator);
            }
        } catch (Exception e) {
            logger.warn("unable to start v1 replication " + channel, e);
        }
    }

    @Override
    public void notifyWatchers() {
        watchManager.notifyWatcher(REPLICATOR_WATCHER_PATH);
    }

}

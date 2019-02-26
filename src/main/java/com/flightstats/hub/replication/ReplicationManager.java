package com.flightstats.hub.replication;

import com.flightstats.hub.cluster.WatchManager;
import com.flightstats.hub.cluster.Watcher;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.model.BuiltInTag;
import com.flightstats.hub.model.ChannelConfig;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.curator.framework.api.CuratorEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.flightstats.hub.app.HubServices.TYPE;
import static com.flightstats.hub.app.HubServices.register;

@Singleton
public class ReplicationManager {
    private final static Logger logger = LoggerFactory.getLogger(ReplicationManager.class);
    private static final String REPLICATOR_WATCHER_PATH = "/replicator/watcher";
    private final Map<String, ChannelReplicator> channelReplicatorMap = new HashMap<>();
    private final AtomicBoolean stopped = new AtomicBoolean();
    private final ExecutorService executor = Executors.newSingleThreadExecutor(
            new ThreadFactoryBuilder().setNameFormat("ReplicationManager").build());
    private final ExecutorService executorPool = Executors.newFixedThreadPool(40,
            new ThreadFactoryBuilder().setNameFormat("ReplicationManager-%d").build());
    @Inject
    private ChannelService channelService;
    @Inject
    private WatchManager watchManager;

    public ReplicationManager() {
        register(new ReplicationService(), TYPE.AFTER_HEALTHY_START, TYPE.PRE_STOP);
    }

    @VisibleForTesting
    ReplicationManager(ChannelService channelService, WatchManager watchManager) {
        this();
        this.channelService = channelService;
        this.watchManager = watchManager;
    }

    private void startManager() {
        logger.info("starting");
        ReplicationManager manager = this;
        watchManager.register(new Watcher() {
            @Override
            public void callback(CuratorEvent event) {
                executor.submit(manager::manageChannels);
            }

            @Override
            public String getPath() {
                return REPLICATOR_WATCHER_PATH;
            }
        });
        ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(
                new ThreadFactoryBuilder().setNameFormat("ReplicationManager-hourly").build());
        scheduledExecutorService.scheduleAtFixedRate(manager::manageChannels, 0, 1, TimeUnit.HOURS);
    }

    private void manageChannels() {
        if (stopped.get()) {
            logger.info("replication stopped");
            return;
        }
        logger.info("starting checks for replication");
        replicateChannels();
        logger.info("completed checks for replication");
    }

    private synchronized void replicateChannels() {
        Set<String> replicators = new HashSet<>();
        Iterable<ChannelConfig> replicatedChannels = channelService.getChannels(BuiltInTag.REPLICATED.toString(), false);
        logger.info("replicating channels {}", replicatedChannels);
        for (ChannelConfig channel : replicatedChannels) {
            logger.info("replicating channel {}", channel.getDisplayName());
            try {
                processChannel(replicators, channel);
            } catch (Exception e) {
                logger.warn("error trying to replicate " + channel, e);
            }
        }
        executorPool.submit(() -> stopAndRemove(replicators, channelReplicatorMap));
    }

    private void processChannel(Set<String> replicators, ChannelConfig channel) {
        String name = channel.getDisplayName();
        replicators.add(name);
        if (channelReplicatorMap.containsKey(name)) {
            ChannelReplicator existingReplicator = channelReplicatorMap.get(name);
            if (!existingReplicator.getChannel().getReplicationSource().equals(channel.getReplicationSource())) {
                ChannelReplicator newReplicator = createReplicator(channel);
                executorPool.submit(() -> changeReplication(existingReplicator, newReplicator));
            } else {
                executorPool.submit(existingReplicator::start);
            }
        } else {
            ChannelReplicator channelReplicator = createReplicator(channel);
            executorPool.submit(() -> startReplication(channelReplicator));
        }
    }

    private ChannelReplicator createReplicator(ChannelConfig channel) {
        ChannelReplicator newReplicator = new ChannelReplicator(channel);
        channelReplicatorMap.put(channel.getDisplayName(), newReplicator);
        return newReplicator;
    }

    private void stopAndRemove(Set<String> replicators, Map<String, ? extends Replicator> replicatorMap) {
        Set<String> toStop = new HashSet<>(replicatorMap.keySet());
        toStop.removeAll(replicators);
        logger.info("stopping replicators {}", toStop);
        for (String nameToStop : toStop) {
            logger.info("stopping {}", nameToStop);
            Replicator replicator = replicatorMap.remove(nameToStop);
            replicator.stop();
        }
    }

    private void changeReplication(ChannelReplicator oldReplicator, ChannelReplicator newReplicator) {
        logger.info("changing replication source from {} to {}",
                oldReplicator.getChannel().getReplicationSource(), newReplicator.getChannel().getReplicationSource());
        oldReplicator.stop();
        startReplication(newReplicator);
    }

    private void startReplication(ChannelReplicator replicator) {
        try {
            logger.debug("starting replication of " + replicator.getChannel().getDisplayName());
            replicator.start();
        } catch (Exception e) {
            channelReplicatorMap.remove(replicator.getChannel().getDisplayName());
            logger.warn("unexpected replication issue " + replicator.getChannel().getDisplayName(), e);
        }
    }

    public void notifyWatchers() {
        watchManager.notifyWatcher(REPLICATOR_WATCHER_PATH);
    }

    private class ReplicationService extends AbstractIdleService {

        @Override
        protected void startUp() throws Exception {
            startManager();
        }

        @Override
        protected void shutDown() throws Exception {
            stopped.set(true);
        }

    }

}

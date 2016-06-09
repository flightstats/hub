package com.flightstats.hub.replication;

import com.flightstats.hub.cluster.WatchManager;
import com.flightstats.hub.cluster.Watcher;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.model.ChannelConfig;
import com.google.common.util.concurrent.AbstractIdleService;
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
import java.util.concurrent.atomic.AtomicBoolean;

import static com.flightstats.hub.app.HubServices.TYPE;
import static com.flightstats.hub.app.HubServices.register;

/**
 * Replication is moving from one Hub into another Hub
 * We will presume we are moving forward in time.
 * There are two flavors of replication:
 * A - setting replicationSource on a Channel, where replicationSource is the fully qualified channel name.
 * B - using a global channel with satellites.
 * <p>
 * Secnario A:
 * Producers are inserting Items into a Hub channel
 * HubA is setup to Replicate a channel from HubB
 * Replication starts at the item after now, and then stays up to date, with some minimal amount of lag.
 * Lag is a minimum of 'app.stable_seconds'.
 * <p>
 * Scenario B:
 * Producers are inserting Items into a Global Hub channel
 * The Global Master manages the replication to the Satellites.
 */
@Singleton
public class ReplicationGlobalManager {
    private final static Logger logger = LoggerFactory.getLogger(ReplicationGlobalManager.class);
    private static final String REPLICATOR_WATCHER_PATH = "/replicator/watcher";

    private ChannelService channelService;
    private WatchManager watchManager;

    private final Map<String, Replicator> channelReplicatorMap = new HashMap<>();
    private final Map<String, Replicator> globalReplicatorMap = new HashMap<>();
    private final AtomicBoolean stopped = new AtomicBoolean();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Inject
    public ReplicationGlobalManager(ChannelService channelService, WatchManager watchManager) {
        this.channelService = channelService;
        this.watchManager = watchManager;
        register(new ReplicationGlobalService(), TYPE.AFTER_HEALTHY_START, TYPE.PRE_STOP);
    }

    private void startManager() {
        logger.info("starting");
        ReplicationGlobalManager manager = this;
        watchManager.register(new Watcher() {
            @Override
            public void callback(CuratorEvent event) {
                executor.submit(manager::replicateAndGlobal);
            }

            @Override
            public String getPath() {
                return REPLICATOR_WATCHER_PATH;
            }
        });

        executor.submit(manager::replicateAndGlobal);
    }

    private void replicateAndGlobal() {
        if (stopped.get()) {
            logger.info("replication stopped");
            return;
        }
        logger.info("starting checks for replication and global");
        //todo - gfm - 6/8/16 - starting channels & groups should probably be async.
        replicateGlobal();
        replicateChannels();
        logger.info("completed checks for replication and global");
    }

    private synchronized void replicateGlobal() {
        Set<String> replicators = new HashSet<>();
        Iterable<ChannelConfig> globalChannels = channelService.getChannels(Replicator.GLOBAL);
        logger.info("replicating global channels {}", globalChannels);
        for (ChannelConfig channel : globalChannels) {
            logger.info("replicating global channel {}", channel);
            if (channel.isGlobalMaster()) {
                try {
                    processGlobal(replicators, channel);
                } catch (Exception e) {
                    logger.warn("unable to do global replication" + channel, e);
                }
            }
        }
        stopAndRemove(replicators, globalReplicatorMap);
        logger.info("completed global");
    }

    private void processGlobal(Set<String> replicators, ChannelConfig channel) {
        for (String satellite : channel.getGlobal().getSatellites()) {
            logger.info("creating satellite {} {}", satellite, channel.getName());
            GlobalReplicator replicator = new GlobalReplicator(channel, satellite);
            replicators.add(replicator.getKey());
            if (!globalReplicatorMap.containsKey(replicator.getKey())) {
                replicator.start();
                globalReplicatorMap.put(replicator.getKey(), replicator);
            }
        }
    }

    private synchronized void replicateChannels() {
        Set<String> replicators = new HashSet<>();
        Iterable<ChannelConfig> replicatedChannels = channelService.getChannels(Replicator.REPLICATED);
        logger.info("replicating channels {}", replicatedChannels);
        for (ChannelConfig channel : replicatedChannels) {
            logger.info("replicating channel {}", channel.getName());
            try {
                processChannel(replicators, channel);
            } catch (Exception e) {
                logger.warn("error trying to replicate " + channel, e);
            }
        }
        stopAndRemove(replicators, channelReplicatorMap);
    }

    private void processChannel(Set<String> replicators, ChannelConfig channel) {
        replicators.add(channel.getName());
        if (channelReplicatorMap.containsKey(channel.getName())) {
            ChannelReplicator replicator = (ChannelReplicator) channelReplicatorMap.get(channel.getName());
            if (!replicator.getChannel().getReplicationSource().equals(channel.getReplicationSource())) {
                logger.info("changing replication source from {} to {}",
                        replicator.getChannel().getReplicationSource(), channel.getReplicationSource());
                replicator.stop();
                startReplication(channel);
            }
        } else {
            startReplication(channel);
        }
    }

    private void stopAndRemove(Set<String> replicators, Map<String, Replicator> replicatorMap) {
        Set<String> toStop = new HashSet<>(replicatorMap.keySet());
        toStop.removeAll(replicators);
        logger.info("stopping replicators {}", toStop);
        for (String nameToStop : toStop) {
            logger.info("stopping {}", nameToStop);
            Replicator replicator = replicatorMap.remove(nameToStop);
            replicator.stop();
        }
    }

    private void startReplication(ChannelConfig channel) {
        logger.debug("starting replication of " + channel);
        ChannelReplicator channelReplicator = new ChannelReplicator(channel);
        channelReplicator.start();
        channelReplicatorMap.put(channel.getName(), channelReplicator);
    }

    public void notifyWatchers() {
        watchManager.notifyWatcher(REPLICATOR_WATCHER_PATH);
    }

    private class ReplicationGlobalService extends AbstractIdleService {

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

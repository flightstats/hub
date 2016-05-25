package com.flightstats.hub.replication;

import com.flightstats.hub.cluster.WatchManager;
import com.flightstats.hub.cluster.Watcher;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.util.HubUtils;
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
    public static final String REPLICATED = "replicated";
    public static final String GLOBAL = "global";
    private static final String REPLICATOR_WATCHER_PATH = "/replicator/watcher";
    private final static Logger logger = LoggerFactory.getLogger(ReplicationGlobalManager.class);

    private final ChannelService channelService;
    private final HubUtils hubUtils;
    private final WatchManager watchManager;
    private final Map<String, ChannelReplicator> replicatorMap = new HashMap<>();
    private final Map<String, GlobalReplicator> globalMasterMap = new HashMap<>();
    private final AtomicBoolean stopped = new AtomicBoolean();
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    @Inject
    public ReplicationGlobalManager(ChannelService channelService, HubUtils hubUtils, WatchManager watchManager) {
        this.channelService = channelService;
        this.hubUtils = hubUtils;
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
        globalChannels();
        replicateChannels();
    }

    private synchronized void globalChannels() {
        logger.info("global channels");
        Iterable<ChannelConfig> globalChannels = channelService.getChannels(GLOBAL);
        for (ChannelConfig channel : globalChannels) {
            if (channel.isGlobalMaster()) {
                //todo - gfm - 5/25/16 - modify globalMasterMap
                //todo - gfm - 5/25/16 - create satellite channels
                //todo - gfm - 5/25/16 - set up replication to satellites
                //todo - gfm - 5/25/16 - stop any removed satellites
            }
        }
        //todo - gfm - 5/25/16 - stop any global channels that aren't found

    }

    private synchronized void replicateChannels() {
        logger.info("replicating channels");
        Set<String> replicators = new HashSet<>();
        Iterable<ChannelConfig> replicatedChannels = channelService.getChannels(REPLICATED);
        for (ChannelConfig channel : replicatedChannels) {
            logger.info("replicating channel {}", channel.getName());
            try {
                replicators.add(channel.getName());
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
            } catch (Exception e) {
                logger.warn("error trying to replicate " + channel, e);
            }
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

    private void startReplication(ChannelConfig channel) {
        logger.debug("starting replication of " + channel);
        try {
            ChannelReplicator channelReplicator = new ChannelReplicator(channel, hubUtils);
            channelReplicator.start();
            replicatorMap.put(channel.getName(), channelReplicator);
        } catch (Exception e) {
            logger.warn("unable to start replication " + channel, e);
        }
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

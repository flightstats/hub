package com.flightstats.hub.cluster;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.app.ShutdownManager;
import com.flightstats.hub.health.HubHealthCheck;
import com.flightstats.hub.util.Sleeper;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.AbstractScheduledService;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.UnknownHostException;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static com.flightstats.hub.app.HubServices.TYPE.AFTER_HEALTHY_START;
import static com.flightstats.hub.app.HubServices.TYPE.PRE_STOP;
import static com.flightstats.hub.app.HubServices.register;

@Singleton
public class DynamicSpokeCluster implements Cluster, Ring {

    private static final Logger logger = LoggerFactory.getLogger(DynamicSpokeCluster.class);
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final CuratorFramework curator;
    private HubHealthCheck healthCheck;
    private final CuratorCluster spokeCluster;
    private final ShutdownManager shutdownManager;

    private volatile SpokeRings spokeRings;

    private final PathChildrenCache eventsCache;
    private final PathChildrenCache decommisionCache;

    private static final String SPOKE_CLUSTER_EVENTS = "/SpokeClusterEvents";
    private static final String DECOMMISION_EVENTS = "/DecommisionEvents";

    @Inject
    public DynamicSpokeCluster(CuratorFramework curator, HubHealthCheck healthCheck,
                               @Named("SpokeCuratorCluster") CuratorCluster spokeCluster,
                               ShutdownManager shutdownManager) throws Exception {
        this.curator = curator;
        this.healthCheck = healthCheck;
        this.spokeCluster = spokeCluster;
        this.shutdownManager = shutdownManager;
        eventsCache = new PathChildrenCache(curator, SPOKE_CLUSTER_EVENTS, true);
        eventsCache.start(PathChildrenCache.StartMode.BUILD_INITIAL_CACHE);
        decommisionCache = new PathChildrenCache(curator, DECOMMISION_EVENTS, true);
        decommisionCache.start(PathChildrenCache.StartMode.BUILD_INITIAL_CACHE);
        createChildWatcher();
        addSpokeClusterListener();
        handleChanges();
        register(new DynamicSpokeClusterService(), AFTER_HEALTHY_START, PRE_STOP);
    }

    private class DynamicSpokeClusterService extends AbstractScheduledService {

        @Override
        protected synchronized void runOneIteration() {
            Collection<ClusterEvent> oldEvents = spokeRings.generateOld(getClusterEvents());
            logger.info("cleaning up old cluster info {}", oldEvents);
            for (ClusterEvent oldEvent : oldEvents) {
                delete(oldEvent);
            }
            logger.info("cleaned up old cluster info");
        }

        @Override
        protected AbstractScheduledService.Scheduler scheduler() {
            return Scheduler.newFixedDelaySchedule(0, 6, TimeUnit.HOURS);
        }

    }

    private void delete(ClusterEvent oldEvent) {
        try {
            String fullPath = SPOKE_CLUSTER_EVENTS + "/" + oldEvent.encode();
            logger.info("deleted {}", fullPath);
            curator.delete().forPath(fullPath);
        } catch (Exception e) {
            logger.warn("unable to delete " + oldEvent, e);
        }
    }

    private void createChildWatcher() {
        eventsCache.getListenable().addListener(
                (client, event) -> {
                    logger.info("event {} {}", event, SPOKE_CLUSTER_EVENTS);
                    if (event.getType().equals(PathChildrenCacheEvent.Type.CHILD_ADDED)) {
                        handleChanges();
                    }
                }, executor);
    }

    private void handleChanges() {
        try {
            Set<ClusterEvent> sortedEvents = getClusterEvents();
            logger.info("kids {}", sortedEvents);
            SpokeRings newRings = new SpokeRings();
            newRings.process(sortedEvents);
            logger.info("rings {}", newRings);
            spokeRings = newRings;
        } catch (Exception e) {
            logger.warn("unable to process Spoke Cluster Change", e);
        }
    }

    private Set<ClusterEvent> getClusterEvents() {
        List<ChildData> currentData = eventsCache.getCurrentData();
        Set<ClusterEvent> sortedEvents = ClusterEvent.set();
        for (ChildData data : currentData) {
            sortedEvents.add(new ClusterEvent(data.getPath(), data.getStat().getMtime()));
        }
        return sortedEvents;
    }

    private void addSpokeClusterListener() {
        spokeCluster.addListener((client, event) -> {
            if (event.getType().equals(PathChildrenCacheEvent.Type.CHILD_REMOVED)) {
                addZkPath(event, false);
            } else if (event.getType().equals(PathChildrenCacheEvent.Type.CHILD_ADDED)) {
                addZkPath(event, true);
            }
        }, executor);
    }

    private void addZkPath(PathChildrenCacheEvent event, boolean added) {
        String nodeName = new String(event.getData().getData());
        long ctime = event.getData().getStat().getCtime();
        String path = SPOKE_CLUSTER_EVENTS + "/" + ClusterEvent.encode(nodeName, ctime, added);
        logger.debug("adding path {} ", path);
        try {
            curator.create()
                    .creatingParentsIfNeeded()
                    .withMode(CreateMode.PERSISTENT)
                    .forPath(path);
        } catch (KeeperException.NodeExistsException e) {
            logger.debug("node already exists " + e.getMessage());
        } catch (Exception e) {
            logger.warn("unexpected " + nodeName, e);
        }

    }

    @Override
    public Collection<String> getLocalServer() throws UnknownHostException {
        return spokeCluster.getLocalServer();
    }

    /**
     * All Servers can include decommission servers.
     */
    @Override
    public Set<String> getAllServers() {
        Set<String> allServers = spokeCluster.getAllServers();
        getDecommissioned(allServers);
        return allServers;
    }

    private void getDecommissioned(Set<String> allServers) {
        List<ChildData> currentData = decommisionCache.getCurrentData();
        for (ChildData childData : currentData) {
            allServers.add(new String(childData.getData()));
        }
    }

    @Override
    public Set<String> getServers(String channel) {
        return getServers(() -> spokeRings.getServers(channel));
    }

    @Override
    public Set<String> getServers(String channel, DateTime pointInTime) {
        return getServers(() -> spokeRings.getServers(channel, pointInTime));
    }

    @Override
    public Set<String> getServers(String channel, DateTime startTime, DateTime endTime) {
        return getServers(() -> spokeRings.getServers(channel, startTime, endTime));
    }

    /**
     * Stop writes to Spoke
     * Allow this server to still get reads and queries
     * Shut down in Spoke TTL minutes
     */
    public void decommission() {
        //todo - gfm - write a file to the system?
        //todo - gfm - if the system starts with the file, recreate the ephemeral node
        //todo - gfm - restart shutdown thread
        String host = spokeCluster.getHost(false);
        try {
            healthCheck.decommission();
            curator.create()
                    .creatingParentsIfNeeded()
                    .withMode(CreateMode.EPHEMERAL)
                    .forPath(DECOMMISION_EVENTS + "/" + host, host.getBytes());
            Sleeper.sleep(500);
            spokeCluster.delete();
            Executors.newSingleThreadExecutor().submit(this::sleepAndShutdown);
        } catch (Exception e) {
            logger.warn("we cant decommission " + host, e);
            throw new RuntimeException(e);
        }
    }

    private void sleepAndShutdown() {
        String host = spokeCluster.getHost(false);
        try {
            int ttlMinutes = HubProperties.getSpokeTtlMinutes() + 1;
            logger.info("sleeping for " + ttlMinutes);
            Sleeper.sleep(TimeUnit.MILLISECONDS.convert(ttlMinutes, TimeUnit.MINUTES));
            logger.info("slept for " + ttlMinutes + ".  Shutting down");
            String path = DECOMMISION_EVENTS + "/" + host;
            curator.delete().forPath(path);
            logger.info("deleted " + path);
            shutdownManager.shutdown(false);
        } catch (Exception e) {
            logger.warn("unable to sleepAndShutdown " + host, e);
        }
    }

    private Set<String> getServers(Supplier<Set<String>> supplier) {
        Set<String> allServers = getAllServers();
        if (allServers.size() <= 3) {
            return allServers;
        }
        return Sets.intersection(allServers, supplier.get());
    }

    public void status(ObjectNode root) {
        ArrayNode active = root.putArray("active");
        Set<String> allServers = spokeCluster.getAllServers();
        for (String server : allServers) {
            active.add(server);
        }

        TreeSet<String> decommissioned = new TreeSet<>();
        getDecommissioned(decommissioned);
        ArrayNode decomm = root.putArray("decommissioned");
        for (String decommiss : decommissioned) {
            decomm.add(decommiss);
        }
        spokeRings.status(root);
    }

    @Override
    public String toString() {
        return "DynamicSpokeCluster{" +
                "spokeRings=" + spokeRings +
                '}';
    }
}

package com.flightstats.hub.cluster;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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

    private CuratorCluster spokeCluster;
    private CuratorFramework curator;
    private volatile SpokeRings spokeRings;

    private final PathChildrenCache eventsCache;

    private static final String PATH = "/SpokeClusterEvents";

    @Inject
    public DynamicSpokeCluster(CuratorFramework curator,
                               @Named("SpokeCuratorCluster") CuratorCluster spokeCluster) throws Exception {
        this.curator = curator;
        this.spokeCluster = spokeCluster;
        eventsCache = new PathChildrenCache(curator, PATH, true);
        eventsCache.start(PathChildrenCache.StartMode.BUILD_INITIAL_CACHE);
        createChildWatcher();
        addSpokeClusterListener();
        handleChanges(true);
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
            String fullPath = PATH + "/" + oldEvent.encode();
            logger.info("deleted {}", fullPath);
            curator.delete().forPath(fullPath);
        } catch (Exception e) {
            logger.warn("unable to delete " + oldEvent, e);
        }
    }

    private void createChildWatcher() {
        eventsCache.getListenable().addListener(
                (client, event) -> {
                    logger.info("event {} {}", event, PATH);
                    if (event.getType().equals(PathChildrenCacheEvent.Type.CHILD_ADDED)) {
                        handleChanges(true);
                    }
                }, executor);
    }

    private void handleChanges(boolean assign) {
        try {
            Set<ClusterEvent> sortedEvents = getClusterEvents();
            logger.info("kids {}", sortedEvents);
            SpokeRings newRings = new SpokeRings();
            newRings.process(sortedEvents);
            logger.info("rings {}", newRings);
            if (assign) {
                spokeRings = newRings;
            }
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
        String path = PATH + "/" + ClusterEvent.encode(nodeName, ctime, added);
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

    @Override
    public Set<String> getAllServers() {
        return spokeCluster.getAllServers();
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

    private Set<String> getServers(Supplier<Set<String>> supplier) {
        Set<String> allServers = spokeCluster.getAllServers();
        if (allServers.size() <= 3) {
            return allServers;
        }
        return Sets.intersection(allServers, supplier.get());
    }

    public void status(ObjectNode root) {
        ArrayNode active = root.putArray("active");
        Set<String> allServers = getAllServers();
        for (String server : allServers) {
            active.add(server);
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

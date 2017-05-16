package com.flightstats.hub.cluster;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.UnknownHostException;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Singleton
public class DynamicSpokeCluster implements Cluster {

    private static final Logger logger = LoggerFactory.getLogger(DynamicSpokeCluster.class);
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

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
        handleChange();
    }

    private void createChildWatcher() {
        eventsCache.getListenable().addListener(
                (client, event) -> {
                    logger.info("event {} {}", event, PATH);
                    if (event.getType().equals(PathChildrenCacheEvent.Type.CHILD_ADDED)) {
                        handleChange();
                    }
                }, executor);
    }

    private void handleChange() {

        try {
            List<ChildData> currentData = eventsCache.getCurrentData();
            Set<ClusterEvent> sortedEvents = ClusterEvent.set();
            for (ChildData data : currentData) {
                sortedEvents.add(new ClusterEvent(data.getPath(), data.getStat().getMtime()));
            }
            logger.info("kids {}", sortedEvents);
            SpokeRings newRings = new SpokeRings();
            newRings.process(sortedEvents);
            logger.info("rings {}", newRings);
            spokeRings = newRings;
        } catch (Exception e) {
            logger.warn("unable to process Spoke Change", e);
        }
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
    public Set<String> getCurrentServers(String channel) {
        Set<String> servers = spokeCluster.getAllServers();
        if (servers.size() <= 3) {
            return servers;
        }

        //todo - gfm - add in rings
        return null;
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

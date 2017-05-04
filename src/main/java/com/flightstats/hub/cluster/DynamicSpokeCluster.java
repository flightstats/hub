package com.flightstats.hub.cluster;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.CuratorEvent;
import org.apache.curator.framework.api.CuratorEventType;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.zookeeper.CreateMode;
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
    private WatchManager watchManager;
    private CuratorFramework curator;
    private volatile SpokeRings spokeRings;

    private static final String PATH = "/SpokeClusterEvents";

    @Inject
    public DynamicSpokeCluster(CuratorFramework curator,
                               @Named("SpokeCuratorCluster") CuratorCluster spokeCluster,
                               WatchManager watchManager) {
        this.curator = curator;
        this.spokeCluster = spokeCluster;
        this.watchManager = watchManager;
        createChildWatcher();
        addSpokeClusterListener();
        handleChange();
    }

    private void createChildWatcher() {
        watchManager.register(new Watcher() {
            @Override
            public void callback(CuratorEvent event) {
                logger.debug("watcher callback {}", event);
                if (event.getType().equals(CuratorEventType.WATCHED)) {
                    logger.debug("watched {}", event.getType());
                    handleChange();
                }
            }

            @Override
            public String getPath() {
                return PATH;
            }

            @Override
            public boolean watchChildren() {
                return true;
            }
        });
    }

    private void handleChange() {

        try {
            List<String> children = curator.getChildren().forPath(PATH);
            logger.info("kids {}", children);
            SpokeRings newRings = new SpokeRings();
            newRings.process(children);
            logger.info("rings {}", newRings);
            spokeRings = newRings;
        } catch (Exception e) {
            logger.warn("unable to process Spoke Change");
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

    private void addZkPath(PathChildrenCacheEvent event, boolean added) throws Exception {
        String nodeName = new String(event.getData().getData());
        long ctime = event.getData().getStat().getCtime();
        String path = PATH + "/" + ClusterEvent.encode(nodeName, ctime, added);
        logger.debug("adding path {} ", path);
        curator.create()
                .creatingParentsIfNeeded()
                .withMode(CreateMode.PERSISTENT)
                .forPath(path);
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

}

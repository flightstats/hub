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
    }

    private void createChildWatcher() {
        watchManager.register(new Watcher() {
            @Override
            public void callback(CuratorEvent event) {
                logger.info("watcher callback {}", event);
                //todo - gfm - when there is a new event, recreate the SpokeRings
                if (event.getType().equals(CuratorEventType.WATCHED)) {
                    logger.info("watched {}", event.getType());
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

        } catch (Exception e) {
            logger.warn("unable to process Spoke Change");
        }
    }

    private void addSpokeClusterListener() {
        spokeCluster.addListener((client, event) -> {
            if (event.getType().equals(PathChildrenCacheEvent.Type.CHILD_REMOVED)) {
                addZkPath(event, "REMOVED");
            } else if (event.getType().equals(PathChildrenCacheEvent.Type.CHILD_ADDED)) {
                addZkPath(event, "ADDED");
            }
        }, executor);
    }

    private void addZkPath(PathChildrenCacheEvent event, String type) throws Exception {
        String nodeName = new String(event.getData().getData());
        long ctime = event.getData().getStat().getCtime();
        logger.info("event {}", event);
        logger.info("deets {} ctime {} ", nodeName, ctime);
        //todo - gfm - consolidate this parsing
        String path = PATH + "/" + nodeName + "|" + ctime + "|" + type;
        logger.info("adding path {} ", path);
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
        return null;
    }

}

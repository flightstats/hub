package com.flightstats.hub.cluster;

import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.util.StringUtils;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

@Singleton
public class CuratorCluster implements Cluster {

    private final static Logger logger = LoggerFactory.getLogger(CuratorCluster.class);

    private final static int WRITE_FACTOR = HubProperties.getProperty("spoke.write.factor", 3);
    private final CuratorFramework curator;
    private final String clusterPath;
    private final boolean useName;
    private DecommissionCluster decommissionCluster;
    private final PathChildrenCache clusterCache;
    private String fullPath;

    @Inject
    public CuratorCluster(CuratorFramework curator, String clusterPath, boolean useName,
                          DecommissionCluster decommissionCluster) throws Exception {
        this.curator = curator;
        this.clusterPath = clusterPath;
        this.useName = useName;
        this.decommissionCluster = decommissionCluster;
        clusterCache = new PathChildrenCache(curator, clusterPath, true);
        clusterCache.start(PathChildrenCache.StartMode.BUILD_INITIAL_CACHE);
    }

    public void addRemovalListener(Consumer<PathChildrenCacheEvent> consumer) {
        addListener((client, event) -> {
            logger.debug("event {} {}", event, clusterPath);
            if (event.getType().equals(PathChildrenCacheEvent.Type.CHILD_REMOVED)
                    || event.getType().equals(PathChildrenCacheEvent.Type.CONNECTION_RECONNECTED)) {
                consumer.accept(event);
            }
        }, Executors.newSingleThreadExecutor());
    }

    public void addCacheListener() {
        addListener((client, event) -> {
            logger.debug("event {} {}", event, clusterPath);
            if (event.getType().equals(PathChildrenCacheEvent.Type.CONNECTION_RECONNECTED)) {
                register();
            }
        });
    }

    private void addListener(PathChildrenCacheListener listener) {
        addListener(listener, MoreExecutors.directExecutor());
    }

    private void addListener(PathChildrenCacheListener listener, Executor executor) {
        clusterCache.getListenable().addListener(listener, executor);
    }

    public void register() throws UnknownHostException {
        String host = Cluster.getHost(useName);
        try {
            logger.info("registering host {} {}", host, clusterPath);
            curator.create().withMode(CreateMode.EPHEMERAL).forPath(getFullPath(), host.getBytes());
        } catch (KeeperException.NodeExistsException e) {
            logger.warn("node already exists {} {} - not likely in prod", host, clusterPath);
        } catch (Exception e) {
            logger.error("unable to register, should die", host, clusterPath, e);
            throw new RuntimeException(e);
        }
    }

    private String getFullPath() throws UnknownHostException {
        fullPath = clusterPath + "/" + Cluster.getHost(useName) + StringUtils.randomAlphaNumeric(6);
        return fullPath;
    }

    @Override
    public Collection<String> getLocalServer() throws UnknownHostException {
        List<String> server = new ArrayList<>();
        server.add(Cluster.getHost(false));
        return server;
    }

    public List<String> getWriteServers() {
        List<String> servers = decommissionCluster.filter(getAllServers());
        if (servers.size() <= WRITE_FACTOR) {
            return servers;
        } else {
            Collections.shuffle(servers);
            return servers.subList(0, WRITE_FACTOR);
        }
    }

    @Override
    public Set<String> getAllServers() {
        Set<String> servers = new HashSet<>();
        List<ChildData> currentData = clusterCache.getCurrentData();
        for (ChildData childData : currentData) {
            servers.add(new String(childData.getData()));
        }
        if (servers.isEmpty()) {
            logger.warn("returning empty collection");
        }
        return servers;
    }

    public List<String> getRandomServers() {
        List<String> servers = new ArrayList<>(getAllServers());
        Collections.shuffle(servers);
        return servers;
    }

    @Override
    public Set<String> getServers(String channel) {
        return getAllServers();
    }

    public void delete() {
        try {
            logger.info("removing host from cluster {} {}", Cluster.getHost(useName), fullPath);
            curator.delete().forPath(fullPath);
            logger.info("deleted host from cluster {} {}", Cluster.getHost(useName), fullPath);
        } catch (KeeperException.NoNodeException e) {
            logger.info("no node for" + fullPath);
        } catch (Exception e) {
            logger.warn("unable to delete " + fullPath, e);
        }
    }

}

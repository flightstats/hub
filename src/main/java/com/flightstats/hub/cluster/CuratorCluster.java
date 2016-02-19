package com.flightstats.hub.cluster;

import com.flightstats.hub.app.HubHost;
import com.flightstats.hub.app.HubProperties;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.UnknownHostException;
import java.util.*;

@Singleton
public class CuratorCluster {

    private final static Logger logger = LoggerFactory.getLogger(CuratorCluster.class);
    private final CuratorFramework curator;
    private final String clusterPath;
    private final boolean useName;
    private final PathChildrenCache clusterCache;
    private String fullPath;

    @Inject
    public CuratorCluster(CuratorFramework curator, String clusterPath, boolean useName) throws Exception {
        this.curator = curator;
        this.clusterPath = clusterPath;
        this.useName = useName;
        clusterCache = new PathChildrenCache(curator, clusterPath, true);
        clusterCache.start(PathChildrenCache.StartMode.BUILD_INITIAL_CACHE);
    }

    public void addCacheListener() {
        clusterCache.getListenable().addListener((client, event) -> {
            logger.info("event {} {}", event, clusterPath);
            if (event.getType().equals(PathChildrenCacheEvent.Type.CONNECTION_RECONNECTED)) {
                register();
            }
        });
    }

    public void register() throws UnknownHostException {
        String host = getHost(useName);
        try {
            logger.info("registering host {} {}", host, clusterPath);
            curator.create().withMode(CreateMode.EPHEMERAL).forPath(getFullPath(true), host.getBytes());
        } catch (KeeperException.NodeExistsException e) {
            logger.warn("node already exists {} {} - not likely in prod", host, clusterPath);
        } catch (Exception e) {
            logger.error("unable to register, should die", host, clusterPath, e);
            throw new RuntimeException(e);
        }
    }

    private String getFullPath(boolean create) throws UnknownHostException {
        if (create) {
            fullPath = clusterPath + "/" + getHost(useName) + RandomStringUtils.randomAlphanumeric(6);
        }
        return fullPath;
    }

    private static String getHost(boolean useName) {
        if (HubProperties.getProperty("app.encrypted", false) || useName) {
            return HubHost.getLocalNamePort();
        } else {
            return HubHost.getLocalAddressPort();
        }
    }

    public static Collection<String> getLocalServer() throws UnknownHostException {
        List<String> server = new ArrayList<>();
        server.add(getHost(false));
        return server;
    }

    public Set<String> getServers() {
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
        List<String> servers = new ArrayList<>(getServers());
        Collections.shuffle(servers);
        return servers;
    }

    public List<String> getRandomRemoteServers() {
        List<String> servers = getRandomServers();
        servers.remove(getHost(useName));
        return servers;
    }

    public void delete() {
        try {
            logger.info("removing host from cluster {} {}", getHost(useName), fullPath);
            curator.delete().forPath(fullPath);
            logger.info("deleted host from cluster {} {}", getHost(useName), fullPath);
        } catch (KeeperException.NoNodeException e) {
            logger.info("no node for" + fullPath);
        } catch (Exception e) {
            logger.warn("unable to delete " + fullPath, e);
        }
    }

}

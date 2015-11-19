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
    private final PathChildrenCache clusterCache;
    private String fullPath;

    @Inject
    public CuratorCluster(CuratorFramework curator, String clusterPath) throws Exception {
        this.curator = curator;
        this.clusterPath = clusterPath;
        clusterCache = new PathChildrenCache(curator, clusterPath, true);
        clusterCache.start(PathChildrenCache.StartMode.BUILD_INITIAL_CACHE);
    }

    public void addCacheListener() {
        clusterCache.getListenable().addListener((client, event) -> {
            logger.info("event {}", event);
            if (event.getType().equals(PathChildrenCacheEvent.Type.CONNECTION_RECONNECTED)) {
                register();
            }
        });
    }

    public void register() throws UnknownHostException {
        try {
            logger.info("registering host {}", getHost());
            curator.create().withMode(CreateMode.EPHEMERAL).forPath(getFullPath(true), getHost().getBytes());
        } catch (KeeperException.NodeExistsException e) {
            logger.warn("node already exists {} - not likely in prod", getHost());
        } catch (Exception e) {
            logger.error("unable to register, should die", getHost(), e);
            throw new RuntimeException(e);
        }
    }

    private String getFullPath(boolean create) throws UnknownHostException {
        if (create) {
            fullPath = clusterPath + "/" + getHost() + RandomStringUtils.randomAlphanumeric(6);
        }
        return fullPath;
    }

    private static String getHost() throws UnknownHostException {
        if (HubProperties.getProperty("app.encrypted", false)) {
            return HubHost.getLocalNamePort();
        } else {
            return HubHost.getLocalAddressPort();
        }
    }

    public static Collection<String> getLocalServer() throws UnknownHostException {
        List<String> server = new ArrayList<>();
        server.add(getHost());
        return server;
    }

    public Collection<String> getServers() {
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

    public Collection<String> getRandomServers() {
        List<String> servers = new ArrayList<>(getServers());
        Collections.shuffle(servers);
        return servers;
    }

    public void delete() {
        try {
            logger.info("removing host from cluster {} {}", getHost(), fullPath);
            curator.delete().forPath(fullPath);
            logger.info("deleted host from cluster {} {}", getHost(), fullPath);
        } catch (KeeperException.NoNodeException e) {
            logger.info("no node for" + fullPath);
        } catch (Exception e) {
            logger.warn("unable to delete " + fullPath, e);
        }
    }

}

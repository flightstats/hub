package com.flightstats.hub.spoke;

import com.flightstats.hub.app.HubHost;
import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.app.HubServices;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.lang3.RandomStringUtils;
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
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class CuratorSpokeCluster {
    public static final String CLUSTER_PATH = "/SpokeCluster";
    private final static Logger logger = LoggerFactory.getLogger(CuratorSpokeCluster.class);
    private final CuratorFramework curator;
    private final PathChildrenCache clusterCache;
    private final Map<String, SpokeServer> spokeServers = new ConcurrentHashMap<>();

    @Inject
    public CuratorSpokeCluster(CuratorFramework curator) throws Exception {
        this.curator = curator;
        clusterCache = new PathChildrenCache(curator, CLUSTER_PATH, true);
        clusterCache.start(PathChildrenCache.StartMode.BUILD_INITIAL_CACHE);
        clusterCache.getListenable().addListener(new PathChildrenCacheListener() {
            @Override
            public void childEvent(CuratorFramework client, PathChildrenCacheEvent event) throws Exception {
                logger.info("event {}", event);
                if (event.getType().equals(PathChildrenCacheEvent.Type.CONNECTION_RECONNECTED)) {
                    register();
                }
                if (event.getType().equals(PathChildrenCacheEvent.Type.CHILD_ADDED)) {
                    ChildData data = event.getData();
                    String server = new String(data.getData());
                    logger.info("registering new SpokeServer", server);
                    spokeServers.put(server, new SpokeServer(server));
                }
            }
        });
        HubServices.register(new CuratorSpokeClusterHook(), HubServices.TYPE.FINAL_POST_START, HubServices.TYPE.PRE_STOP);
    }

    public void register() throws UnknownHostException {
        try {
            logger.info("registering host {}", getHost());
            curator.create().withMode(CreateMode.EPHEMERAL).forPath(getFullPath(), getHost().getBytes());
        } catch (KeeperException.NodeExistsException e) {
            logger.warn("node already exists {} - not likely in prod", getHost());
        } catch (Exception e) {
            logger.error("unable to register, should die", getHost(), e);
            throw new RuntimeException(e);
        }
    }

    private String getFullPath() throws UnknownHostException {
        return CLUSTER_PATH + "/" + getHost() + RandomStringUtils.randomAlphanumeric(6);
    }

    private static String getHost() throws UnknownHostException {
        if (HubProperties.getProperty("app.encrypted", false)) {
            return HubHost.getLocalNamePort();
        } else {
            return HubHost.getLocalAddressPort();
        }
    }

    public Collection<SpokeServer> getLocalServer() throws UnknownHostException {
        List<SpokeServer> server = new ArrayList<>();
        server.add(getSpokeServer(getHost()));
        return server;
    }

    public Collection<SpokeServer> getServers() {
        Set<SpokeServer> servers = new HashSet<>();
        List<ChildData> currentData = clusterCache.getCurrentData();
        for (ChildData childData : currentData) {
            String server = new String(childData.getData());
            SpokeServer spokeServer = getSpokeServer(server);
            if (spokeServer.isEligible()) {
                servers.add(spokeServer);
            }
        }
        if (servers.isEmpty()) {
            logger.warn("returning empty collection");
        }
        return servers;
    }

    public SpokeServer getSpokeServer(String server) {
        SpokeServer spokeServer = spokeServers.get(server);
        if (spokeServer == null) {
            spokeServers.put(server, new SpokeServer(server));
            return spokeServers.get(server);
        } else {
            return spokeServer;
        }
    }

    public Collection<SpokeServer> getRandomServers() {
        List<SpokeServer> servers = new ArrayList<>(getServers());
        Collections.shuffle(servers);
        return servers;
    }

    private class CuratorSpokeClusterHook extends AbstractIdleService {
        @Override
        protected void startUp() throws Exception {
            register();
        }

        @Override
        protected void shutDown() throws Exception {
            logger.info("removing host from cluster " + getHost());
            curator.delete().forPath(getFullPath());
        }
    }
}

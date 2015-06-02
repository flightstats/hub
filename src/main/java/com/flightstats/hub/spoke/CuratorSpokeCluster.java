package com.flightstats.hub.spoke;

import com.flightstats.hub.app.HubHost;
import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.app.HubServices;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Inject;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class CuratorSpokeCluster implements SpokeCluster {
    public static final String CLUSTER_PATH = "/SpokeCluster";
    private final static Logger logger = LoggerFactory.getLogger(CuratorSpokeCluster.class);
    private final CuratorFramework curator;
    private final PathChildrenCache clusterCache;
    private Set<String> backupCluster = Collections.newSetFromMap(new ConcurrentHashMap<>());

    @Inject
    public CuratorSpokeCluster(CuratorFramework curator) throws Exception {
        this.curator = curator;
        clusterCache = new PathChildrenCache(curator, CLUSTER_PATH, true);
        clusterCache.start(PathChildrenCache.StartMode.BUILD_INITIAL_CACHE);
        clusterCache.getListenable().addListener(new PathChildrenCacheListener() {
            @Override
            public void childEvent(CuratorFramework client, PathChildrenCacheEvent event) throws Exception {
                logger.info("event {}", event);
                if (event.getType().equals(PathChildrenCacheEvent.Type.CHILD_ADDED)) {
                    backupCluster.add(new String(event.getData().getData()));
                }
            }
        });
        HubServices.register(new CuratorSpokeClusterHook(), HubServices.TYPE.POST_START, HubServices.TYPE.PRE_STOP);
    }

    public void startUp() throws UnknownHostException {
        String host = getHost();
        backupCluster.add(host);
        try {
            logger.info("adding host {}", host);
            curator.create().withMode(CreateMode.EPHEMERAL).forPath(getFullPath(), host.getBytes());
        } catch (KeeperException.NodeExistsException e) {
            logger.warn("node already exists {} - not likely in prod", host);
        } catch (Exception e) {
            logger.error("unable to startUp, should die", host, e);
            throw new RuntimeException(e);
        }
        backupCluster.addAll(getServers());
    }

    private String getFullPath() throws UnknownHostException {
        return CLUSTER_PATH + "/" + getHost();
    }

    private String getHost() throws UnknownHostException {
        if (HubProperties.getProperty("app.encrypted", false)) {
            return HubHost.getLocalNamePort();
        } else {
            return HubHost.getLocalAddressPort();
        }
    }

    @Override
    public List<String> getServers() {
        List<String> servers = new ArrayList<>();
        List<ChildData> currentData = clusterCache.getCurrentData();
        for (ChildData childData : currentData) {
            servers.add(new String(childData.getData()));
        }
        if (servers.isEmpty()) {
            logger.warn("returning backup cluster");
            servers.addAll(backupCluster);
        }
        return servers;
    }

    @Override
    public List<String> getRandomServers() {
        List<String> servers = getServers();
        Collections.shuffle(servers);
        return servers;
    }

    private class CuratorSpokeClusterHook extends AbstractIdleService {
        @Override
        protected void startUp() throws Exception {
            startUp();
        }

        @Override
        protected void shutDown() throws Exception {
            logger.info("removing host from cluster " + getHost());
            curator.delete().forPath(getFullPath());
        }
    }
}

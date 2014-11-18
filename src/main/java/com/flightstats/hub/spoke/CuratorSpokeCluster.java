package com.flightstats.hub.spoke;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.zookeeper.CreateMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

public class CuratorSpokeCluster implements SpokeCluster {
    public static final String CLUSTER_PATH = "/SpokeCluster";
    private final static Logger logger = LoggerFactory.getLogger(CuratorSpokeCluster.class);
    private final CuratorFramework curator;
    private final int port;
    private final PathChildrenCache clusterCache;

    @Inject
    public CuratorSpokeCluster(CuratorFramework curator, @Named("http.bind_port") int port) {
        this.curator = curator;
        this.port = port;
        //todo - gfm - 11/18/14 - we need to register this server on the path ASAP after servicing requests.
        //todo - gfm - 11/18/14 - should spoke run on different port from the Hub, which starts first?
        clusterCache = new PathChildrenCache(curator, CLUSTER_PATH, true);
        clusterCache.getListenable().addListener(new PathChildrenCacheListener() {
            @Override
            public void childEvent(CuratorFramework client, PathChildrenCacheEvent event) throws Exception {
                logger.info("event {}", event);
            }
        });
    }

    public void register() {
        try {
            clusterCache.start(PathChildrenCache.StartMode.BUILD_INITIAL_CACHE);
            String host = InetAddress.getLocalHost().getHostName() + ":" + port;
            logger.info("adding host {}", host);
            curator.create().withMode(CreateMode.EPHEMERAL).forPath(CLUSTER_PATH + "/" + host, host.getBytes());
        } catch (Exception e) {
            logger.error("unable to register, should die", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<String> getServers() {
        List<String> servers = new ArrayList<>();
        List<ChildData> currentData = clusterCache.getCurrentData();
        //noinspection Convert2streamapi
        for (ChildData childData : currentData) {
            servers.add(new String(childData.getData()));
        }
        return servers;
    }
}

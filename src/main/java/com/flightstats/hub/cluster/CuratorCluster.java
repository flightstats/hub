package com.flightstats.hub.cluster;

import com.flightstats.hub.app.HubHost;
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;

@Singleton
public class CuratorCluster {

    private final static Logger logger = LoggerFactory.getLogger(CuratorCluster.class);

    private final int spokeWriteFactor;
    private final CuratorFramework curator;
    private final String clusterPath;
    private final boolean useName;
    private boolean checkReadOnly;
    private DecommissionCluster decommissionCluster;
    private final HubProperties hubProperties;
    private final PathChildrenCache clusterCache;
    private String fullPath;

    @Inject
    public CuratorCluster(CuratorFramework curator,
                          String clusterPath,
                          boolean useName,
                          boolean checkReadOnly,
                          DecommissionCluster decommissionCluster,
                          HubProperties hubProperties) throws Exception
    {
        this.curator = curator;
        this.clusterPath = clusterPath;
        this.useName = useName;
        this.checkReadOnly = checkReadOnly;
        this.decommissionCluster = decommissionCluster;
        this.hubProperties = hubProperties;
        this.spokeWriteFactor = hubProperties.getProperty("spoke.write.factor", 3);
        clusterCache = new PathChildrenCache(curator, clusterPath, true);
        clusterCache.start(PathChildrenCache.StartMode.BUILD_INITIAL_CACHE);
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

    public void register() {
        if (checkReadOnly && hubProperties.isReadOnly()) {
            logger.info("this hub is read only, not registering");
            return;
        }
        String host = getHost(useName);
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

    private String getFullPath() {
        fullPath = clusterPath + "/" + getHost(useName) + StringUtils.randomAlphaNumeric(6);
        return fullPath;
    }

    public List<String> getWriteServers() {
        List<String> servers = decommissionCluster.filter(getAllServers());
        if (servers.size() <= spokeWriteFactor) {
            return servers;
        } else {
            Collections.shuffle(servers);
            return servers.subList(0, spokeWriteFactor);
        }
    }

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

    public Set<String> getServers(String channel) {
        return getAllServers();
    }

    public List<String> getRemoteServers(String channel) {
        List<String> servers = new ArrayList<>(getServers(channel));
        servers.remove(getHost(true));
        return servers;
    }

    public Collection<String> getLocalServer() {
        List<String> server = new ArrayList<>();
        server.add(getHost(false));
        return server;
    }

    String getHost(boolean useName) {
        if (useName) {
            return HubHost.getLocalNamePort();
        } else {
            return HubHost.getLocalAddressPort();
        }
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

package com.flightstats.hub.cluster;

import com.flightstats.hub.config.properties.AppProperties;
import com.flightstats.hub.config.properties.LocalHostProperties;
import com.flightstats.hub.config.properties.SpokeProperties;
import com.flightstats.hub.util.StringUtils;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;

@Singleton
@Slf4j
public class CuratorCluster implements Cluster {

    private final CuratorFramework curator;
    private final String clusterPath;
    private final boolean useName;
    private final boolean checkReadOnly;
    private final DecommissionCluster decommissionCluster;
    private final AppProperties appProperties;
    private final SpokeProperties spokeProperties;
    private LocalHostProperties localHostProperties;

    private String fullPath;
    private final PathChildrenCache clusterCache;

    @Inject
    public CuratorCluster(CuratorFramework curator,
                          String clusterPath,
                          boolean useName,
                          boolean checkReadOnly,
                          DecommissionCluster decommissionCluster,
                          AppProperties appProperties,
                          SpokeProperties spokeProperties,
                          LocalHostProperties localHostProperties) throws Exception {
        this.curator = curator;
        this.clusterPath = clusterPath;
        this.useName = useName;
        this.checkReadOnly = checkReadOnly;
        this.decommissionCluster = decommissionCluster;
        clusterCache = new PathChildrenCache(curator, clusterPath, true);
        clusterCache.start(PathChildrenCache.StartMode.BUILD_INITIAL_CACHE);
        this.appProperties = appProperties;
        this.spokeProperties = spokeProperties;
        this.localHostProperties = localHostProperties;
    }

    public void addCacheListener() {
        addListener((client, event) -> {
            log.debug("event {} {}", event, clusterPath);
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
        if (checkReadOnly && appProperties.isReadOnly()) {
            log.info("this hub is read only, not registering");
            return;
        }
        try {
            log.info("registering host {} {}", localHostProperties.getHost(useName), clusterPath);
            curator.create().withMode(CreateMode.EPHEMERAL).forPath(getFullPath(), localHostProperties.getHost(useName).getBytes());
        } catch (KeeperException.NodeExistsException e) {
            log.warn("node already exists {} {} - not likely in prod", localHostProperties.getHost(useName), clusterPath);
        } catch (Exception e) {
            log.error("unable to register, should die {} {}", localHostProperties.getHost(useName), clusterPath, e);
            throw new RuntimeException(e);
        }
    }

    private String getFullPath() {
        fullPath = clusterPath + "/" + localHostProperties.getHost(useName) + StringUtils.randomAlphaNumeric(6);
        return fullPath;
    }

    public List<String> getWriteServers() {
        List<String> servers = decommissionCluster.filter(getAllServers());
        if (servers.size() <= spokeProperties.getWriteFactor()) {
            return servers;
        } else {
            Collections.shuffle(servers);
            return servers.subList(0, spokeProperties.getWriteFactor());
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
            log.warn("returning empty collection");
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
            log.info("removing host from cluster {} {}", localHostProperties.getHost(useName), fullPath);
            curator.delete().forPath(fullPath);
            log.info("deleted host from cluster {} {}", localHostProperties.getHost(useName), fullPath);
        } catch (KeeperException.NoNodeException e) {
            log.info("no node for" + fullPath);
        } catch (Exception e) {
            log.warn("unable to delete " + fullPath, e);
        }
    }

    public List<String> getRemoteServers(String channel) {
        List<String> servers = new ArrayList<>(getServers(channel));
        servers.remove(localHostProperties.getHost(true));
        return servers;
    }

}

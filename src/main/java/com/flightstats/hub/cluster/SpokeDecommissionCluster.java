package com.flightstats.hub.cluster;

import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.util.TimeUtil;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.data.Stat;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Singleton
public class SpokeDecommissionCluster implements DecommissionCluster {

    private static final Logger logger = LoggerFactory.getLogger(SpokeDecommissionManager.class);

    private final CuratorFramework curator;
    private final PathChildrenCache withinSpokeCache;

    private static final String WITHIN_SPOKE = "/SpokeDecommission/withinSpokeTtl";
    private static final String DO_NOT_RESTART = "/SpokeDecommission/doNotRestart";

    @Inject
    public SpokeDecommissionCluster(CuratorFramework curator) throws Exception {
        this.curator = curator;
        withinSpokeCache = new PathChildrenCache(curator, WITHIN_SPOKE, true);
        withinSpokeCache.start(PathChildrenCache.StartMode.BUILD_INITIAL_CACHE);
    }

    void decommission() throws Exception {
        logger.info("decommissioning" + withinSpokeKey());
        if (!withinSpokeExists() && !doNotRestartExists()) {
            logger.info("creating " + withinSpokeKey());
            curator.create().creatingParentsIfNeeded().forPath(withinSpokeKey(), getHost().getBytes());
        }
        logger.info("decommission started " + withinSpokeKey());
    }

    boolean withinSpokeExists() throws Exception {
        return withinSpokeStat() != null;
    }

    private Stat withinSpokeStat() throws Exception {
        return curator.checkExists().forPath(withinSpokeKey());
    }

    boolean doNotRestartExists() throws Exception {
        return curator.checkExists().forPath(doNotRestartKey()) != null;
    }

    private String withinSpokeKey() {
        return WITHIN_SPOKE + "/" + getHost();
    }

    private String doNotRestartKey() {
        return DO_NOT_RESTART + "/" + getHost();
    }

    private String getHost() {
        return Cluster.getHost(false);
    }

    void commission(String server) throws Exception {
        //todo - gfm - check to see if the server is running ???
        deleteQuietly(WITHIN_SPOKE + "/" + server);
        deleteQuietly(DO_NOT_RESTART + "/" + server);
    }

    public List<String> getWithinSpokeTTL() {
        List<String> servers = new ArrayList<>();
        List<ChildData> currentData = withinSpokeCache.getCurrentData();
        for (ChildData childData : currentData) {
            servers.add(new String(childData.getData()));
        }
        return servers;
    }

    public List<String> getDoNotRestart() throws Exception {
        return curator.getChildren().forPath(DO_NOT_RESTART);
    }


    public List<String> filter(Set<String> servers) {
        List<String> filteredServers = new ArrayList<>(servers);
        List<ChildData> currentData = withinSpokeCache.getCurrentData();
        for (ChildData childData : currentData) {
            String server = new String(childData.getData());
            if (servers.contains(server)) {
                filteredServers.remove(server);
            }
        }
        return filteredServers;
    }

    long getDoNotRestartMinutes() throws Exception {
        DateTime creationTime = new DateTime(withinSpokeStat().getCtime(), DateTimeZone.UTC);
        DateTime ttlDateTime = TimeUtil.now().minusMinutes(HubProperties.getSpokeTtlMinutes());
        return new Duration(ttlDateTime, creationTime).getStandardMinutes();
    }

    void doNotRestart() {
        try {
            createQuietly(doNotRestartKey(), getHost().getBytes());
            logger.info("deleting key " + withinSpokeKey());
            deleteQuietly(withinSpokeKey());
            logger.info("doNotRestart complete");
        } catch (Exception e) {
            logger.warn("unable to complete ", e);
        }
    }

    private void createQuietly(String key, byte[] bytes) throws Exception {
        try {
            logger.info("creating key " + key);
            curator.create().creatingParentsIfNeeded().forPath(key, bytes);
        } catch (KeeperException.NodeExistsException e) {
            logger.info(" key already exists " + key);
        }
    }

    private void deleteQuietly(String key) throws Exception {
        try {
            logger.info("deleting key " + key);
            curator.delete().forPath(key);
        } catch (Exception e) {
            logger.info(" issue trying to delete " + key, e);
        }
    }
}

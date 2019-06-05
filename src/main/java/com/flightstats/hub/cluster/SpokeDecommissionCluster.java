package com.flightstats.hub.cluster;

import com.flightstats.hub.config.properties.LocalHostProperties;
import com.flightstats.hub.config.properties.SpokeProperties;
import com.flightstats.hub.spoke.SpokeStore;
import com.flightstats.hub.util.TimeUtil;

import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.data.Stat;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Singleton
@Slf4j
public class SpokeDecommissionCluster implements DecommissionCluster {
    
    private static final String WITHIN_SPOKE = "/SpokeDecommission/withinSpokeTtl";
    private static final String DO_NOT_RESTART = "/SpokeDecommission/doNotRestart";
    private final CuratorFramework curator;
    private final PathChildrenCache withinSpokeCache;
    private final SpokeProperties spokeProperties;
    private final LocalHostProperties localHostProperties;

    @Inject
    public SpokeDecommissionCluster(CuratorFramework curator,
                                    SpokeProperties spokeProperties,
                                    LocalHostProperties localHostProperties) throws Exception {
        this.curator = curator;
        this.spokeProperties = spokeProperties;
        this.localHostProperties = localHostProperties;
        withinSpokeCache = new PathChildrenCache(curator, WITHIN_SPOKE, true);
        withinSpokeCache.start(PathChildrenCache.StartMode.BUILD_INITIAL_CACHE);
    }

    void initialize() throws Exception {
        byte[] bytes = getLocalhost().getBytes();
        createQuietly(WITHIN_SPOKE, bytes);
        createQuietly(DO_NOT_RESTART, bytes);
    }

    void decommission() throws Exception {
        String server = getLocalhost();
        log.info("decommissioning" + withinSpokeKey(server));
        if (!withinSpokeExists(server) && !doNotRestartExists(server)) {
            log.info("creating " + withinSpokeKey(server));
            curator.create().creatingParentsIfNeeded().forPath(withinSpokeKey(server), server.getBytes());
        }
        log.info("decommission started " + withinSpokeKey(server));
    }

    boolean withinSpokeExists() throws Exception {
        return withinSpokeExists(getLocalhost());
    }

    private boolean withinSpokeExists(String server) throws Exception {
        return withinSpokeStat(server) != null;
    }

    private Stat withinSpokeStat(String server) throws Exception {
        return curator.checkExists().forPath(withinSpokeKey(server));
    }

    boolean doNotRestartExists() throws Exception {
        return doNotRestartExists(getLocalhost());
    }

    private boolean doNotRestartExists(String server) throws Exception {
        return curator.checkExists().forPath(doNotRestartKey(server)) != null;
    }

    private String withinSpokeKey(String server) {
        return WITHIN_SPOKE + "/" + server;
    }

    private String doNotRestartKey(String server) {
        return DO_NOT_RESTART + "/" + server;
    }

    private String getLocalhost() {
        return localHostProperties.getHost(false);
    }

    void recommission(String server) throws Exception {
        if (!doNotRestartExists(server) && !withinSpokeExists(server)) {
            throw new RuntimeException("server " + server + "does not have zookeeper keys.");
        }
        deleteQuietly(withinSpokeKey(server));
        deleteQuietly(doNotRestartKey(server));
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
        DateTime creationTime = new DateTime(withinSpokeStat(getLocalhost()).getCtime(), DateTimeZone.UTC);
        DateTime ttlDateTime = TimeUtil.now().minusMinutes(spokeProperties.getTtlMinutes(SpokeStore.WRITE));
        return new Duration(ttlDateTime, creationTime).getStandardMinutes();
    }

    void doNotRestart() {
        try {
            String localhost = getLocalhost();
            createQuietly(doNotRestartKey(localhost), localhost.getBytes());
            log.info("deleting key " + withinSpokeKey(localhost));
            deleteQuietly(withinSpokeKey(localhost));
            log.info("doNotRestart complete");
        } catch (Exception e) {
            log.warn("unable to complete ", e);
        }
    }

    private void createQuietly(String key, byte[] bytes) throws Exception {
        try {
            log.info("creating key " + key);
            curator.create().creatingParentsIfNeeded().forPath(key, bytes);
        } catch (KeeperException.NodeExistsException e) {
            log.info(" key already exists " + key);
        }
    }

    private void deleteQuietly(String key) {
        try {
            log.info("deleting key " + key);
            curator.delete().forPath(key);
        } catch (Exception e) {
            log.info(" issue trying to delete " + key, e);
        }
    }
}

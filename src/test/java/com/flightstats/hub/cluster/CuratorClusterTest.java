package com.flightstats.hub.cluster;

import com.flightstats.hub.config.AppProperties;
import com.flightstats.hub.config.PropertiesLoader;
import com.flightstats.hub.config.SpokeProperties;
import com.flightstats.hub.test.Integration;
import com.flightstats.hub.util.Sleeper;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


@Slf4j
class CuratorClusterTest {

    private static CuratorFramework curator;

    @BeforeAll
    static void setUpClass() throws Exception {
        curator = Integration.startZooKeeper();
    }

    @Test
    void testPath() throws Exception {
        log.info("starting testPath");

        final CuratorCluster clusterTest = new CuratorCluster(curator,
                "/SpokeCluster",
                false,
                true,
                new SpokeDecommissionCluster(curator, new SpokeProperties(PropertiesLoader.getInstance())),
                new AppProperties(PropertiesLoader.getInstance()),
                new SpokeProperties(PropertiesLoader.getInstance()));

        final CuratorCluster cluster = new CuratorCluster(curator,
                "/CuratorClusterTest",
                false,
                true,
                new SpokeDecommissionCluster(curator, new SpokeProperties(PropertiesLoader.getInstance())),
                new AppProperties(PropertiesLoader.getInstance()),
                new SpokeProperties(PropertiesLoader.getInstance()));

        Collection<String> servers = cluster.getAllServers();
        assertNotNull(servers);
        assertEquals(0, servers.size());
        log.info("got expected 0");
        cluster.register();
        Sleeper.sleep(5000);
        log.info("slept 5000");
        servers = cluster.getAllServers();
        assertNotNull(servers);
        assertEquals(1, servers.size());
        log.info("got expected 1");
        cluster.delete();
        Sleeper.sleep(5000);
        servers = cluster.getAllServers();
        assertNotNull(servers);
        assertEquals(0, servers.size());
    }

}
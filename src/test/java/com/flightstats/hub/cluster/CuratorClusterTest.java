package com.flightstats.hub.cluster;

import com.flightstats.hub.config.AppProperties;
import com.flightstats.hub.config.PropertiesLoader;
import com.flightstats.hub.config.SpokeProperties;
import com.flightstats.hub.test.Integration;
import com.flightstats.hub.util.Sleeper;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@Slf4j
public class CuratorClusterTest {

    private static CuratorFramework curator;

    @BeforeClass
    public static void setUpClass() throws Exception {
        curator = Integration.startZooKeeper();
    }

    @Test
    public void testPath() throws Exception {
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
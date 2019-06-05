package com.flightstats.hub.cluster;

import com.flightstats.hub.config.properties.AppProperties;
import com.flightstats.hub.config.properties.LocalHostProperties;
import com.flightstats.hub.config.properties.PropertiesLoader;
import com.flightstats.hub.config.properties.SpokeProperties;
import com.flightstats.hub.config.properties.SystemProperties;
import com.flightstats.hub.test.IntegrationTestSetup;
import com.flightstats.hub.util.Sleeper;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


@Slf4j
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CuratorClusterTest {
    private final SpokeProperties spokeProperties = new SpokeProperties(PropertiesLoader.getInstance());
    private final AppProperties appProperties = new AppProperties(PropertiesLoader.getInstance());
    private final SystemProperties systemProperties = new SystemProperties(PropertiesLoader.getInstance());
    private final LocalHostProperties localHostProperties = new LocalHostProperties(appProperties, systemProperties);
    private CuratorFramework curator;

    @BeforeAll
    void setup() {
        curator = IntegrationTestSetup.run().getZookeeperClient();
    }

    @Test
    void testPath() throws Exception {
        log.info("starting testPath");

        final CuratorCluster clusterTest = new CuratorCluster(curator,
                "/SpokeCluster",
                false,
                true,
                new SpokeDecommissionCluster(curator, spokeProperties, localHostProperties),
                appProperties,
                spokeProperties,
                localHostProperties);

        final CuratorCluster cluster = new CuratorCluster(curator,
                "/CuratorClusterTest",
                false,
                true,
                new SpokeDecommissionCluster(curator, spokeProperties, localHostProperties),
                appProperties,
                spokeProperties,
                localHostProperties);

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
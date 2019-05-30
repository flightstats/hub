package com.flightstats.hub.cluster;

import com.flightstats.hub.config.properties.AppProperties;
import com.flightstats.hub.config.properties.LocalHostProperties;
import com.flightstats.hub.config.properties.PropertiesLoader;
import com.flightstats.hub.config.properties.SpokeProperties;
import com.flightstats.hub.config.properties.SystemProperties;
import com.flightstats.hub.spoke.SpokeStore;
import com.flightstats.hub.test.IntegrationTestSetup;
import org.apache.curator.framework.CuratorFramework;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SpokeDecommissionClusterTest {
    private final SpokeProperties spokeProperties = new SpokeProperties(PropertiesLoader.getInstance());
    private final AppProperties appProperties = new AppProperties(PropertiesLoader.getInstance());
    private final SystemProperties systemProperties = new SystemProperties(PropertiesLoader.getInstance());
    private final LocalHostProperties localHostProperties = new LocalHostProperties(appProperties, systemProperties);
    private SpokeDecommissionCluster cluster;

    @BeforeAll
    void setUpClass() throws Exception {
        CuratorFramework curator = IntegrationTestSetup.run().getZookeeperClient();
        cluster = new SpokeDecommissionCluster(curator, spokeProperties, localHostProperties);
    }

    @AfterEach
    void afterTest() {
        cluster.doNotRestart();
    }

    @Test
    void testDecommission() throws Exception {
        cluster.decommission();
        assertTrue(cluster.withinSpokeExists());
        assertFalse(cluster.doNotRestartExists());

        assertEquals(spokeProperties.getTtlMinutes(SpokeStore.WRITE), cluster.getDoNotRestartMinutes(), 1);

        cluster.doNotRestart();
        assertFalse(cluster.withinSpokeExists());
        assertTrue(cluster.doNotRestartExists());
    }
}
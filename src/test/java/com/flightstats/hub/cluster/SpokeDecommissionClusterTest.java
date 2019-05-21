package com.flightstats.hub.cluster;

import com.flightstats.hub.config.properties.PropertiesLoader;
import com.flightstats.hub.config.properties.SpokeProperties;
import com.flightstats.hub.spoke.SpokeStore;
import com.flightstats.hub.test.Integration;
import org.apache.curator.framework.CuratorFramework;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpokeDecommissionClusterTest {
    private static final SpokeProperties spokeProperties = new SpokeProperties(PropertiesLoader.getInstance());
    private static SpokeDecommissionCluster cluster;

    @BeforeAll
    static void setUpClass() throws Exception {
        CuratorFramework curator = Integration.startZooKeeper();
        cluster = new SpokeDecommissionCluster(curator,
                new SpokeProperties(PropertiesLoader.getInstance()));
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
package com.flightstats.hub.cluster;

import com.flightstats.hub.config.PropertyLoader;
import com.flightstats.hub.config.SpokeProperty;
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

    private static final SpokeProperty spokeProperty = new SpokeProperty(PropertyLoader.getInstance());
    private static SpokeDecommissionCluster cluster;

    @BeforeAll
    static void setUpClass() throws Exception {
        CuratorFramework curator = Integration.startZooKeeper();
        cluster = new SpokeDecommissionCluster(curator,
                new SpokeProperty(PropertyLoader.getInstance()));
    }

    @AfterEach
    void afterTest() throws Exception {
        cluster.doNotRestart();
    }

    @Test
    void testDecommission() throws Exception {
        cluster.decommission();
        assertTrue(cluster.withinSpokeExists());
        assertFalse(cluster.doNotRestartExists());

        assertEquals(spokeProperty.getTtlMinutes(SpokeStore.WRITE), cluster.getDoNotRestartMinutes(), 1);

        cluster.doNotRestart();
        assertFalse(cluster.withinSpokeExists());
        assertTrue(cluster.doNotRestartExists());
    }
}
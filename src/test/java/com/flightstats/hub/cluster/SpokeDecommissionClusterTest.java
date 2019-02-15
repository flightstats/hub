package com.flightstats.hub.cluster;

import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.spoke.SpokeStore;
import com.flightstats.hub.test.Integration;
import org.apache.curator.framework.CuratorFramework;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SpokeDecommissionClusterTest {

    private static CuratorFramework curator;
    private static SpokeDecommissionCluster cluster;

    @BeforeAll
    public static void setUpClass() throws Exception {
        curator = Integration.startZooKeeper();
        cluster = new SpokeDecommissionCluster(curator);
    }

    @AfterEach
    public void afterTest() throws Exception {
        cluster.doNotRestart();
    }

    @Test
    public void testDecommission() throws Exception {
        cluster.decommission();
        assertTrue(cluster.withinSpokeExists());
        assertFalse(cluster.doNotRestartExists());

        assertEquals(HubProperties.getSpokeTtlMinutes(SpokeStore.WRITE), cluster.getDoNotRestartMinutes(), 1);

        cluster.doNotRestart();
        assertFalse(cluster.withinSpokeExists());
        assertTrue(cluster.doNotRestartExists());
    }
}
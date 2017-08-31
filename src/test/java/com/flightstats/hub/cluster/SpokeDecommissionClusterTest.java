package com.flightstats.hub.cluster;

import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.test.Integration;
import org.apache.curator.framework.CuratorFramework;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

public class SpokeDecommissionClusterTest {

    private static CuratorFramework curator;
    private static SpokeDecommissionCluster cluster;

    @BeforeClass
    public static void setUpClass() throws Exception {
        curator = Integration.startZooKeeper();
        cluster = new SpokeDecommissionCluster(curator);
    }

    @After
    public void afterTest() throws Exception {
        cluster.doNotRestart();
    }

    @Test
    public void testDecommission() throws Exception {
        cluster.decommission();
        assertTrue(cluster.withinSpokeExists());
        assertFalse(cluster.doNotRestartExists());

        assertEquals(HubProperties.getSpokeTtlMinutes(), cluster.getDoNotRestartMinutes(), 1);

        cluster.doNotRestart();
        assertFalse(cluster.withinSpokeExists());
        assertTrue(cluster.doNotRestartExists());
    }
}
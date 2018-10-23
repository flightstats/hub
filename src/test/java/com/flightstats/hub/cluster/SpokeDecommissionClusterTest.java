package com.flightstats.hub.cluster;

import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.spoke.SpokeStore;
import com.flightstats.hub.test.TestMain;
import com.google.inject.Injector;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SpokeDecommissionClusterTest {

    @Test
    public void testDecommission() throws Exception {
        Injector injector = TestMain.start();
        SpokeDecommissionCluster cluster = injector.getInstance(SpokeDecommissionCluster.class);
        HubProperties hubProperties = injector.getInstance(HubProperties.class);

        cluster.decommission();
        assertTrue(cluster.withinSpokeExists());
        assertFalse(cluster.doNotRestartExists());
        assertEquals(hubProperties.getSpokeTtlMinutes(SpokeStore.WRITE), cluster.getDoNotRestartMinutes(), 1);

        cluster.doNotRestart();
        assertFalse(cluster.withinSpokeExists());
        assertTrue(cluster.doNotRestartExists());

        cluster.doNotRestart();
    }

}

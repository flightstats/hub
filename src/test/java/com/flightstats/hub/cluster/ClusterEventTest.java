package com.flightstats.hub.cluster;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ClusterEventTest {

    @Test
    public void testParse() {
        ClusterEvent clusterEvent = new ClusterEvent("10|A|ADDED");
        assertEquals(10, clusterEvent.getTime());
        assertEquals("A", clusterEvent.getName());
        assertTrue(clusterEvent.isAdded());
    }

}
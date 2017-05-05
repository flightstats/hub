package com.flightstats.hub.cluster;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ClusterEventTest {

    @Test
    public void testParse() {
        ClusterEvent clusterEvent = new ClusterEvent("10|A|ADDED", 10);
        assertEquals(10, clusterEvent.getCreationTime());
        assertEquals("A", clusterEvent.getName());
        assertTrue(clusterEvent.isAdded());
    }

    @Test
    public void testSorting() {
        Set<ClusterEvent> events = ClusterEvent.set();
        events.add(new ClusterEvent("10|A|REMOVED", 21));
        events.add(new ClusterEvent("20|B|ADDED", 20));
        events.add(new ClusterEvent("20|B|REMOVED", 30));
        events.add(new ClusterEvent("10|A|ADDED", 10));

        List<ClusterEvent> eventsList = new ArrayList<>(events);
        assertEquals("10|A|ADDED", eventsList.get(0).encode());
        assertEquals("20|B|ADDED", eventsList.get(1).encode());
        assertEquals("10|A|REMOVED", eventsList.get(2).encode());
        assertEquals("20|B|REMOVED", eventsList.get(3).encode());
    }

}
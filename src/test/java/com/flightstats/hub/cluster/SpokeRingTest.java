package com.flightstats.hub.cluster;

import org.apache.commons.lang3.RandomStringUtils;
import org.joda.time.DateTime;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SpokeRingTest {

    private SpokeRing spokeRing;

    private void createRing(int nodes) {
        spokeRing = new SpokeRing(new DateTime(), createNodes(nodes));
    }

    private String[] createNodes(int nodes) {
        String[] spokeNodes = new String[nodes];
        for (int i = 0; i < nodes; i++) {
            spokeNodes[i] = "n" + i;
        }
        return spokeNodes;
    }

    @Test
    public void testOrdering() {
        createRing(4);
        checkNodes("A", "n0", "n1", "n2");
        checkNodes("B", "n2", "n3", "n0");
        checkNodes("E", "n1", "n2", "n3");
        checkNodes("G", "n3", "n0", "n1");

        createRing(6);
        checkNodes("A", "n0", "n4", "n5");
        checkNodes("B", "n1", "n2", "n3");
        checkNodes("G", "n2", "n3", "n0");
        checkNodes("F", "n3", "n0", "n4");
        checkNodes("I", "n4", "n5", "n1");
        checkNodes("C", "n5", "n1", "n2");

        createRing(8);
        checkNodes("A", "n0", "n4", "n5");
        checkNodes("C", "n1", "n7", "n2");
        checkNodes("G", "n2", "n6", "n3");
        checkNodes("F", "n3", "n0", "n4");
        checkNodes("I", "n4", "n5", "n1");
        checkNodes("E", "n5", "n1", "n7");
        checkNodes("a", "n6", "n3", "n0");
        checkNodes("B", "n7", "n2", "n6");
    }

    private void checkNodes(String channel, String... expected) {
        checkNodes(channel, spokeRing.getNodes(channel), expected);
    }

    private void checkNodes(String channel, Collection<String> nodes, String... expected) {
        List<String> nodesFound = new ArrayList<>(nodes);
        assertEquals(3, nodesFound.size());
        System.out.println("found " + channel + " " + nodesFound);
        assertEquals(expected[0], nodesFound.get(0));
        assertEquals(expected[1], nodesFound.get(1));
        assertEquals(expected[2], nodesFound.get(2));
    }

    //todo - gfm - why is this failing?

    /*@Test
    public void testDistribution() {
        for (int i = 3; i <= 12; i++) {
            runDistribution(i);
        }
    }*/

    private void runDistribution(int nodes) {
        createRing(nodes);
        int loops = 100 * 1000;
        Map<String, AtomicInteger> countMap = new HashMap<>();
        for (int i = 0; i < loops; i++) {
            String random = RandomStringUtils.randomAlphanumeric(6);
            Collection<String> spokeNodes = spokeRing.getNodes(random);
            assertEquals(3, spokeNodes.size());
            String spokeNode = spokeNodes.iterator().next();
            AtomicInteger integer = countMap.getOrDefault(spokeNode, new AtomicInteger(0));
            integer.incrementAndGet();
            countMap.put(spokeNode, integer);
        }
        for (Map.Entry<String, AtomicInteger> entry : countMap.entrySet()) {
            assertEquals(loops / nodes, entry.getValue().get(), loops * .007);
        }
    }

    @Test
    public void testCurrentRing() {
        DateTime start = new DateTime();
        spokeRing = new SpokeRing(start.minusHours(1), createNodes(4));
        checkNodes("A", spokeRing.getNodes("A", start), "n0", "n1", "n2");
        checkNodes("A", spokeRing.getNodes("A", start.minusMinutes(59)), "n0", "n1", "n2");
        checkNodes("A", spokeRing.getNodes("A", start.minusMinutes(60)), "n0", "n1", "n2");

        checkNodes("A", spokeRing.getNodes("A", start.minusHours(5), start.minusMinutes(59)), "n0", "n1", "n2");

        assertTrue(spokeRing.getNodes("A", start.minusMinutes(61)).isEmpty());
        assertTrue(spokeRing.getNodes("A", start.minusDays(61)).isEmpty());
    }

    @Test
    public void testOldRing() {
        DateTime start = new DateTime();
        spokeRing = new SpokeRing(start.minusHours(1), start.minusMinutes(20), createNodes(4));

        checkNodes("A", spokeRing.getNodes("A", start.minusMinutes(21)), "n0", "n1", "n2");
        checkNodes("A", spokeRing.getNodes("A", start.minusMinutes(25), start.minusMinutes(20)), "n0", "n1", "n2");
        checkNodes("A", spokeRing.getNodes("A", start.minusMinutes(21), start.minusMinutes(10)), "n0", "n1", "n2");
        checkNodes("A", spokeRing.getNodes("A", start.minusMinutes(62), start.minusMinutes(59)), "n0", "n1", "n2");

        assertTrue(spokeRing.getNodes("A", start).isEmpty());
        assertTrue(spokeRing.getNodes("A", start.minusMinutes(19)).isEmpty());
        assertTrue(spokeRing.getNodes("A", start.minusMinutes(19), start.minusMinutes(18)).isEmpty());
        assertTrue(spokeRing.getNodes("A", start.minusDays(21)).isEmpty());
    }


}
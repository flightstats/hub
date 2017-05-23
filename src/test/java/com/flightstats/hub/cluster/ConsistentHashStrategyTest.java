package com.flightstats.hub.cluster;

import com.flightstats.hub.app.HubProperties;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ConsistentHashStrategyTest {

    private ConsistentHashStrategy strategy;

    @Before
    public void setUp() throws Exception {
        HubProperties.setProperty("consistent.hashing.replicas", "128");
    }

    @Test
    public void testTwo() {
        strategy = new ConsistentHashStrategy(Arrays.asList("A", "B"));
        checkNodes("A", "A", "B");
        checkNodes("B", "A", "B");
        checkNodes("E", "A", "B");
        checkNodes("G", "A", "B");
    }

    @Test
    public void testThree() {
        strategy = new ConsistentHashStrategy(Arrays.asList("A", "B", "C"));
        checkNodes("A", "A", "B", "C");
        checkNodes("B", "A", "B", "C");
        checkNodes("E", "A", "B", "C");
        checkNodes("G", "A", "B", "C");
    }

    @Test
    public void testFour() {
        strategy = new ConsistentHashStrategy(Arrays.asList("A", "B", "C", "D"));
        checkNodes("A", "A", "B", "D");
        checkNodes("D", "A", "C", "D");
        checkNodes("F", "B", "C", "D");
        checkNodes("H", "A", "B", "C");
    }

    @Test
    public void testFive() {
        strategy = new ConsistentHashStrategy(Arrays.asList("A", "B", "C", "D", "E"));
        checkNodes("I", "A", "D", "E");
        checkNodes("E", "A", "C", "E");
        checkNodes("B", "A", "B", "D");
        checkNodes("G", "C", "D", "E");
        checkNodes("A", "A", "B", "D");
    }

    private void checkNodes(String channel, String... expected) {
        List<String> nodesFound = new ArrayList<>(strategy.getServers(channel));
        List<String> expectedList = Arrays.asList(expected);
        assertEquals(expectedList.size(), nodesFound.size());
        System.out.println("found " + channel + " found " + nodesFound + " expected " + expectedList);
        assertTrue(nodesFound.containsAll(expectedList));
    }

    @Test
    public void testEvenDistribution() {

        runStrategy(getNodes(3));
        runStrategy(getNodes(4));
        runStrategy(getNodes(5));
        runStrategy(getNodes(6));
        runStrategy(getNodes(7));
        runStrategy(getNodes(9));
        runStrategy(getNodes(11));
        runStrategy(getNodes(13));
    }

    private List<String> getNodes(int count) {
        List<String> nodes = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            nodes.add("10.10.120." + i + ":8080");
        }
        return nodes;
    }

    private void runStrategy(List<String> nodes) {
        ConsistentHashStrategy strategy = new ConsistentHashStrategy(nodes);
        Map<String, AtomicInteger> values = new HashMap<>();

        int loops = 100 * 1000;
        for (int i = 0; i < loops; i++) {
            String channel = RandomStringUtils.randomAlphanumeric(12);
            Set<String> servers = strategy.getServers(channel);
            for (String server : servers) {
                AtomicInteger integer = values.getOrDefault(server, new AtomicInteger());
                integer.addAndGet(1);
                values.put(server, integer);
            }
        }

        for (Map.Entry<String, AtomicInteger> entry : values.entrySet()) {
            int count = entry.getValue().get();
            int expected = loops * 3 / nodes.size();
            assertEquals(expected, count, expected * 0.1);
        }
    }
}
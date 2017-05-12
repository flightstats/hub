package com.flightstats.hub.cluster;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ConsistentHashStrategyTest {

    private ConsistentHashStrategy strategy;

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
}
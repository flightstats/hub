package com.flightstats.hub.cluster;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class EqualRangesStrategyTest {

    private EqualRangesStrategy strategy;

    @Test
    public void testTwo() {
        strategy = new EqualRangesStrategy(Arrays.asList("A", "B"));
        checkNodes("A", "A", "B");
        checkNodes("B", "A", "B");
        checkNodes("E", "A", "B");
        checkNodes("G", "A", "B");
    }

    @Test
    public void testThree() {
        strategy = new EqualRangesStrategy(Arrays.asList("A", "B", "C"));
        checkNodes("A", "A", "B", "C");
        checkNodes("B", "A", "B", "C");
        checkNodes("E", "A", "B", "C");
        checkNodes("G", "A", "B", "C");
    }

    @Test
    public void testFour() {
        strategy = new EqualRangesStrategy(Arrays.asList("A", "D", "C", "B"));
        checkNodes("A", "B", "D", "A");
        checkNodes("B", "D", "C", "B");
        checkNodes("E", "A", "D", "C");
        checkNodes("G", "C", "B", "A");
    }

    @Test
    public void testFive() {
        strategy = new EqualRangesStrategy(Arrays.asList("A", "D", "E", "C", "B"));
        checkNodes("I", "A", "D", "E");
        checkNodes("E", "D", "E", "C");
        checkNodes("B", "E", "C", "B");
        checkNodes("G", "C", "B", "A");
        checkNodes("A", "B", "A", "B");
    }

    private void checkNodes(String channel, String... expected) {
        List<String> nodesFound = new ArrayList<>(strategy.getServers(channel));
        List<String> expectedList = Arrays.asList(expected);
        assertEquals(expectedList.size(), nodesFound.size());
        System.out.println("found " + channel + " found " + nodesFound + " expected " + expectedList);
        assertTrue(nodesFound.containsAll(expectedList));
    }
}
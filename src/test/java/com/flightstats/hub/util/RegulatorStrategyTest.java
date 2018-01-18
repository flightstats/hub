package com.flightstats.hub.util;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RegulatorStrategyTest {
    private RegulatorStrategy strategy;
    private static final int GOAL_MILLIS = 1000;

    @Before
    public void setUp() throws Exception {
        strategy = new RegulatorStrategy();
    }

    @Test
    public void testSameSingleThread() {
        ExecutorState executorState = createExecutorState(1, 0);
        executorState.setStart(executorState.getEnd() - GOAL_MILLIS);
        RegulatorResults results = strategy.calculate(executorState);
        assertEquals(1, results.getThreads());
        assertEquals(0, results.getSleepTime());
        assertTrue(results.getSlowChannels().isEmpty());
    }

    @Test
    public void testSingleThreadMoreSleep() {
        ExecutorState executorState = createExecutorState(1, 0);
        executorState.setStart(executorState.getEnd() - GOAL_MILLIS / 2);
        RegulatorResults results = strategy.calculate(executorState);
        assertEquals(1, results.getThreads());
        assertEquals(166, results.getSleepTime());
        assertTrue(results.getSlowChannels().isEmpty());
    }

    @Test
    public void testSingleThreadLessSleep() {
        ExecutorState executorState = createExecutorState(1, 300);
        executorState.setStart((long) (executorState.getEnd() - GOAL_MILLIS * 1.5));
        RegulatorResults results = strategy.calculate(executorState);
        assertEquals(1, results.getThreads());
        assertEquals(133, results.getSleepTime());
        assertTrue(results.getSlowChannels().isEmpty());
    }

    @Test
    public void testSingleThreadMoreThreads() {
        ExecutorState executorState = createExecutorState(1, 100);
        executorState.setStart((long) (executorState.getEnd() - GOAL_MILLIS * 1.5));
        RegulatorResults results = strategy.calculate(executorState);
        assertEquals(2, results.getThreads());
        assertEquals(0, results.getSleepTime());
        assertTrue(results.getSlowChannels().isEmpty());
    }

    private HashMap<String, Long> createResults() {
        HashMap<String, Long> channelResults = new HashMap<>();
        channelResults.put("one", (long) (GOAL_MILLIS / 2));
        channelResults.put("two", (long) (GOAL_MILLIS / 3));
        channelResults.put("three", (long) (GOAL_MILLIS / 4));
        return channelResults;
    }

    private ExecutorState createExecutorState(int threads, int sleep) {
        ExecutorState executorState = new ExecutorState(GOAL_MILLIS, threads, sleep);
        executorState.setEnd(System.currentTimeMillis());
        executorState.setResults(createResults());
        return executorState;
    }

    @Test
    public void testMultipleThreadMoreThreads() {
        ExecutorState executorState = createExecutorState(2, 0);
        executorState.setStart((long) (executorState.getEnd() - GOAL_MILLIS * 1.5));
        RegulatorResults results = strategy.calculate(executorState);
        assertEquals(3, results.getThreads());
        assertEquals(0, results.getSleepTime());
        assertTrue(results.getSlowChannels().isEmpty());
    }

    @Test
    public void testMultipleThreadLessThreads() {
        ExecutorState executorState = createExecutorState(3, 0);
        executorState.setStart((long) (executorState.getEnd() - GOAL_MILLIS * .5));
        RegulatorResults results = strategy.calculate(executorState);
        assertEquals(2, results.getThreads());
        assertEquals(0, results.getSleepTime());
        assertTrue(results.getSlowChannels().isEmpty());
    }

    //todo - gfm - slow channels

    @Test
    public void testSlowChannels() {
        HashMap<String, Long> channelResults = new HashMap<>();
        channelResults.put("one", (long) (GOAL_MILLIS * 2));
        channelResults.put("two", (long) (GOAL_MILLIS / 3));
        channelResults.put("three", (long) (GOAL_MILLIS * 1.1));

        ExecutorState executorState = new ExecutorState(GOAL_MILLIS, 1, 0);
        executorState.setEnd(System.currentTimeMillis());
        executorState.setResults(channelResults);
        executorState.setStart((long) (executorState.getEnd() - GOAL_MILLIS));
        RegulatorResults results = strategy.calculate(executorState);
        assertEquals(1, results.getThreads());
        assertEquals(0, results.getSleepTime());
        assertTrue(results.getSlowChannels().containsKey("one"));
        assertTrue(results.getSlowChannels().containsKey("three"));
    }

    //todo - gfm - more threads, no gain
}
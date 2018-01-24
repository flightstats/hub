package com.flightstats.hub.util;

import org.junit.Test;

import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RegulatorStrategyTest {
    private static final int GOAL_MILLIS = 1000;

    @Test
    public void testSameSingleThread() {
        ExecutorState executorState = createExecutorState(1, 0, 3);
        executorState.setStart(executorState.getEnd() - GOAL_MILLIS);
        RegulatorResults results = RegulatorStrategy.calculate(executorState);
        assertEquals(1, results.getThreads());
        assertEquals(0, results.getSleepTime());
        assertTrue(results.getSlowChannels().isEmpty());
    }

    @Test
    public void testSingleThreadMoreSleep() {
        ExecutorState executorState = createExecutorState(1, 0, 3);
        executorState.setStart(executorState.getEnd() - GOAL_MILLIS / 2);
        RegulatorResults results = RegulatorStrategy.calculate(executorState);
        assertEquals(1, results.getThreads());
        assertEquals(166, results.getSleepTime());
        assertTrue(results.getSlowChannels().isEmpty());
    }

    @Test
    public void testSingleThreadLessSleep() {
        ExecutorState executorState = createExecutorState(1, 300, 3);
        executorState.setStart((long) (executorState.getEnd() - GOAL_MILLIS * 1.5));
        RegulatorResults results = RegulatorStrategy.calculate(executorState);
        assertEquals(1, results.getThreads());
        assertEquals(133, results.getSleepTime());
        assertTrue(results.getSlowChannels().isEmpty());
    }

    @Test
    public void testSingleThreadMoreThreads() {
        ExecutorState executorState = createExecutorState(1, 100, 3);
        executorState.setStart((long) (executorState.getEnd() - GOAL_MILLIS * 1.5));
        RegulatorResults results = RegulatorStrategy.calculate(executorState);
        assertEquals(2, results.getThreads());
        assertEquals(0, results.getSleepTime());
        assertTrue(results.getSlowChannels().isEmpty());
    }

    private HashMap<String, Long> createResults(int count) {
        HashMap<String, Long> channelResults = new HashMap<>();
        for (int i = 0; i < count; i++) {
            channelResults.put(i + "", (long) (GOAL_MILLIS / (i + 1)));
        }
        return channelResults;
    }

    private ExecutorState createExecutorState(int threads, int sleep, int count) {
        ExecutorState executorState = new ExecutorState(GOAL_MILLIS, threads, sleep);
        executorState.setEnd(System.currentTimeMillis());
        executorState.setResults(createResults(count));
        return executorState;
    }

    @Test
    public void testMultipleThreadMoreThreads() {
        ExecutorState executorState = createExecutorState(2, 0, 3);
        executorState.setStart((long) (executorState.getEnd() - GOAL_MILLIS * 1.5));
        RegulatorResults results = RegulatorStrategy.calculate(executorState);
        assertEquals(3, results.getThreads());
        assertEquals(0, results.getSleepTime());
        assertTrue(results.getSlowChannels().isEmpty());
    }

    @Test
    public void testMultipleThreadLessThreads() {
        ExecutorState executorState = createExecutorState(3, 0, 3);
        executorState.setStart((long) (executorState.getEnd() - GOAL_MILLIS * .5));
        RegulatorResults results = RegulatorStrategy.calculate(executorState);
        assertEquals(2, results.getThreads());
        assertEquals(333, results.getSleepTime());
        assertTrue(results.getSlowChannels().isEmpty());
    }

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
        RegulatorResults results = RegulatorStrategy.calculate(executorState);
        assertEquals(1, results.getThreads());
        assertEquals(0, results.getSleepTime());
        assertTrue(results.getSlowChannels().containsKey("one"));
        assertTrue(results.getSlowChannels().containsKey("three"));
    }

    @Test
    public void testMoreSleepTwoThreads() {
        ExecutorState executorState = createExecutorState(2, 0, 3);
        executorState.setStart((long) (executorState.getEnd() - GOAL_MILLIS * .75));
        RegulatorResults results = RegulatorStrategy.calculate(executorState);
        assertEquals(2, results.getThreads());
        assertEquals(166, results.getSleepTime());
        assertTrue(results.getSlowChannels().isEmpty());
    }

    @Test
    public void testMoreSleepTwoThreads2() {
        ExecutorState executorState = createExecutorState(2, 0, 10);
        executorState.setStart((long) (executorState.getEnd() - GOAL_MILLIS * .75));
        RegulatorResults results = RegulatorStrategy.calculate(executorState);
        assertEquals(2, results.getThreads());
        assertEquals(50, results.getSleepTime());
        assertTrue(results.getSlowChannels().isEmpty());
    }

    @Test
    public void testLessSleepTwoThreads() {
        ExecutorState executorState = createExecutorState(2, 50, 10);
        executorState.setStart((long) (executorState.getEnd() - GOAL_MILLIS * 1.1));
        RegulatorResults results = RegulatorStrategy.calculate(executorState);
        assertEquals(3, results.getThreads());
        assertEquals(20, results.getSleepTime());
        assertTrue(results.getSlowChannels().isEmpty());
    }

}
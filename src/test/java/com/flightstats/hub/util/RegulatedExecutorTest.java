package com.flightstats.hub.util;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class RegulatedExecutorTest {

    @Test
    public void testRightSpeedOneThread() {
        RegulatedConfig config = RegulatedConfig.builder().name("testRightSpeed")
                .startThreads(1)
                .maxThreads(10)
                .percentUtilization(50)
                .timeUnit(TimeUtil.Unit.MILLIS)
                .timeValue(100)
                .build();
        RegulatedExecutor executor = new RegulatedExecutor(config);
        executor.runAsync("zero", () -> Sleeper.sleep(50));
        executor.join();
        assertTrue(executor.getCurrentThreads() <= 2);
        assertTrue(executor.getCurrentThreads() >= 1);
    }

    @Test
    public void testRightSpeedFiveThreads() {
        RegulatedConfig config = RegulatedConfig.builder().name("testRightSpeedFiveThreads")
                .startThreads(5)
                .maxThreads(10)
                .percentUtilization(50)
                .timeUnit(TimeUtil.Unit.MILLIS)
                .timeValue(100)
                .build();
        RegulatedExecutor executor = new RegulatedExecutor(config);
        for (int i = 0; i < 5; i++) {
            executor.runAsync("" + i, () -> Sleeper.sleep(50));
        }
        executor.join();
        assertTrue(executor.getCurrentThreads() <= 6);
        assertTrue(executor.getCurrentThreads() >= 5);
    }

    @Test
    public void testTooFast() {
        RegulatedConfig config = RegulatedConfig.builder().name("testTooFast")
                .startThreads(5)
                .maxThreads(10)
                .percentUtilization(50)
                .timeUnit(TimeUtil.Unit.MILLIS)
                .timeValue(100)
                .build();
        RegulatedExecutor executor = new RegulatedExecutor(config);

        for (int i = 0; i < 5; i++) {
            executor.runAsync("" + i, () -> Sleeper.sleep(10));
        }
        executor.join();
        assertTrue(executor.getCurrentThreads() <= 3);
        assertTrue(executor.getCurrentThreads() >= 1);
    }

    @Test
    public void testTooSlow() {
        RegulatedConfig config = RegulatedConfig.builder().name("testTooFast")
                .startThreads(1)
                .maxThreads(10)
                .percentUtilization(50)
                .timeUnit(TimeUtil.Unit.MILLIS)
                .timeValue(100)
                .build();
        RegulatedExecutor executor = new RegulatedExecutor(config);

        for (int i = 0; i < 5; i++) {
            executor.runAsync("" + i, () -> Sleeper.sleep(20));
        }
        executor.join();
        assertTrue(executor.getCurrentThreads() <= 4);
        assertTrue(executor.getCurrentThreads() >= 2);
    }

    @Test
    public void testZeroPool() {
        RegulatedConfig config = RegulatedConfig.builder().name("testZeroPool")
                .startThreads(1)
                .maxThreads(10)
                .percentUtilization(54)
                .timeUnit(TimeUtil.Unit.MILLIS)
                .timeValue(100)
                .build();
        RegulatedExecutor executor = new RegulatedExecutor(config);

        executor.runAsync("zero", () -> Sleeper.sleep(10));
        executor.join();
        assertTrue(executor.getCurrentThreads() <= 2);
        assertTrue(executor.getCurrentThreads() >= 1);
    }
}
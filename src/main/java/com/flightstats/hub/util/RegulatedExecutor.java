package com.flightstats.hub.util;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The regulated executor is designed to spread out load across a timeUnit of time.
 * It will spread out the load so that, on average, the system will use the percent time allocated to process.
 */
public class RegulatedExecutor {
    private static final Logger logger = LoggerFactory.getLogger(RegulatedExecutor.class);

    private RegulatedConfig config;

    private int currentThreads;
    private long currentSleep = 0;

    private ExecutorService executor;
    private ExecutorState currentState;
    private RegulatorResults results;

    public RegulatedExecutor(RegulatedConfig config) {
        this.config = config;
        currentThreads = config.getStartThreads();
        results = RegulatorResults.builder().threads(currentThreads).sleepTime(currentSleep).build();
        createExecutor();
        createCurrentState();
    }

    private void createCurrentState() {
        currentState = new ExecutorState(getGoalMillis(), currentThreads, currentSleep);
    }

    private void createExecutor() {
        executor = Executors.newFixedThreadPool(currentThreads, new ThreadFactoryBuilder()
                .setNameFormat("S3BatchWriterChannel-%d").build());
    }

    public void runAsync(String name, Runnable runnable) {
        if (currentSleep > 0) {
            Sleeper.sleep(currentSleep);
        }
        currentState.runAsync(name, runnable, results.isSlowChannel(name), executor);
    }

    public void join() {

        currentState.join();

        results = RegulatorStrategy.calculate(currentState);
        int newThreads = results.getThreads();
        if (newThreads != currentThreads) {
            logger.info("changing pool from {} to {}", currentThreads, newThreads);
            currentThreads = newThreads;
            executor.shutdown();
            createExecutor();
            //todo - gfm - is there a way to resize executor w/o recreating?
        }
        currentSleep = results.getSleepTime();
        createCurrentState();
    }

    public int getCurrentThreads() {
        return currentThreads;
    }

    private long getGoalMillis() {
        return (long) ((double) config.getTimeUnit().getDuration().getMillis() *
                config.getTimeValue() * config.getPercentUtilization() / 100);
    }
}

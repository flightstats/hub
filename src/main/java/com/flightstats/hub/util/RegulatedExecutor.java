package com.flightstats.hub.util;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
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
    private ExecutorService executor;
    private RegulatedExecutorState currentState;

    public RegulatedExecutor(RegulatedConfig config) {
        this.config = config;
        currentThreads = config.getStartThreads();
        createExecutor();
        currentState = new RegulatedExecutorState();
    }

    private void createExecutor() {
        executor = Executors.newFixedThreadPool(currentThreads, new ThreadFactoryBuilder()
                .setNameFormat("S3BatchWriterChannel-%d").build());
    }

    public void runAsync(Runnable runnable) {
        currentState.add(CompletableFuture.runAsync(runnable, executor));
    }

    public void join() {
        CompletableFuture.allOf(currentState.getArray()).join();
        currentState.end();
        double ratio = currentState.getRatio();
        logger.info("{} ran {} items with {} threads.  ratio {}",
                config.getName(), currentState.futures.size(), currentThreads, ratio);
        int newThreads = Math.max(1, (int) Math.ceil(ratio * currentThreads));
        if (newThreads != currentThreads) {
            logger.info("changing pool from {} to {}", currentThreads, newThreads);
            currentThreads = newThreads;
            executor.shutdown();
            createExecutor();
            //todo - gfm - is there a way to resize executor w/o recreating?
        }
        currentState = new RegulatedExecutorState();
    }

    class RegulatedExecutorState {

        List<CompletableFuture> futures = new ArrayList<>();
        long start;
        long end;

        void end() {
            end = System.currentTimeMillis();
        }

        long getExecutionTime() {
            return end - start;
        }

        void add(CompletableFuture future) {
            if (start == 0) {
                start = System.currentTimeMillis();
            }
            futures.add(future);
        }

        CompletableFuture[] getArray() {
            return futures.toArray(new CompletableFuture[futures.size()]);
        }

        double getRatio() {
            double goalMillis = (double) config.getTimeUnit().getDuration().getMillis() *
                    config.getTimeValue() * config.getPercentUtilization() / 100;
            logger.info("gaol {} actual {}", goalMillis, getExecutionTime());
            return (double) getExecutionTime() / goalMillis;
        }

    }

    int getCurrentThreads() {
        return currentThreads;
    }
}

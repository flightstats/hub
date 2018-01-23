package com.flightstats.hub.util;

import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@Setter
@Getter
class ExecutorState {

    private static final Logger logger = LoggerFactory.getLogger(ExecutorState.class);

    private List<CompletableFuture> futures = new ArrayList<>();
    private Map<String, Long> results = new HashMap<>();
    private long start;
    private long end;
    private long goalMillis;
    private int threads;
    private long sleep;

    ExecutorState(long goalMillis, int threads, long sleep) {
        this.goalMillis = goalMillis;
        this.threads = threads;
        this.sleep = sleep;
    }

    void join() {
        CompletableFuture.allOf(getArray()).join();
        end = System.currentTimeMillis();
    }

    void runAsync(String name, Runnable runnable, boolean isSlow, ExecutorService executor) {
        if (start == 0) {
            start = System.currentTimeMillis();
        }
        if (isSlow) {
            results.put(name, goalMillis + 1);
            SlowExecutor.runAsync(name, resultsRunnable(name, runnable));
        } else {
            futures.add(CompletableFuture.runAsync(resultsRunnable(name, runnable), executor));
        }
    }

    private Runnable resultsRunnable(String name, Runnable runnable) {
        return () -> {
            long currentTime = System.currentTimeMillis();
            try {
                runnable.run();
            } catch (Exception e) {
                logger.warn("unexpected exception running " + name, e);
            } finally {
                results.put(name, System.currentTimeMillis() - currentTime);
            }
        };
    }

    private CompletableFuture[] getArray() {
        return futures.toArray(new CompletableFuture[futures.size()]);
    }

    double getRatio() {
        long executionTime = end - start;
        logger.info("goal {} actual {}", goalMillis, executionTime);
        return (double) executionTime / goalMillis;
    }

    int getSize() {
        return results.size();
    }

}

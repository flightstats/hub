package com.flightstats.hub.metrics;

import com.flightstats.hub.model.Traces;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;

public class TracesSortedSetTest {

    private final static Logger logger = LoggerFactory.getLogger(TracesSortedSetTest.class);

    @Test
    public void testSingle() {
        int maxSize = 10;
        int count = 100;
        long start = System.currentTimeMillis();
        TracesSortedSet tracesSortedSet = new TracesSortedSet(maxSize);
        addTraces(count, start, tracesSortedSet);
        assertEquals(maxSize, tracesSortedSet.size());
        List<Traces> list = new ArrayList<>(tracesSortedSet);
        for (int i = 0; i < list.size(); i++) {
            assertEquals(count - 1 - i, list.get(i).getTime());
        }
    }

    private void addTraces(int count, long start, TracesSortedSet tracesSortedSet) {
        for (int i = 0; i < count; i++) {
            Traces traces = new Traces(i);
            traces.setStart(start - i);
            traces.setEnd(start);
            tracesSortedSet.add(traces);
        }
    }

    @Test
    public void testMulti() throws InterruptedException {
        int maxSize = 100;
        int count = 1000;
        long start = System.currentTimeMillis();
        TracesSortedSet tracesSortedSet = new TracesSortedSet(maxSize);
        ExecutorService executorService = Executors.newCachedThreadPool();
        AtomicInteger loops = new AtomicInteger();
        AtomicBoolean exception = new AtomicBoolean();
        AtomicLong ids = new AtomicLong();
        executorService.submit(() -> {
            try {
                while (!executorService.isShutdown()) {
                    for (Traces traces : tracesSortedSet) {
                        ids.addAndGet(traces.getTime());
                    }
                    loops.incrementAndGet();
                }
            } catch (Exception e) {
                logger.warn("?", e);
                exception.set(true);
            }
        });

        for (int i = 0; i < 5; i++) {
            executorService.submit(() -> addTraces(count, start, tracesSortedSet));
        }

        executorService.shutdown();
        executorService.awaitTermination(1, TimeUnit.MINUTES);
        logger.info("ids {}", ids);
        for (Traces traces : tracesSortedSet) {
            //traces.log(logger);
            assertTrue(traces.getTime() > 970);
        }
        assertTrue(loops.get() > 100);
        assertFalse(exception.get());

    }

}
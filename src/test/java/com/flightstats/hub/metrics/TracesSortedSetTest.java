package com.flightstats.hub.metrics;

import com.flightstats.hub.util.Sleeper;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
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
            Sleeper.sleep((long) (Math.random()));
        }
    }

    @Test
    public void testMulti() throws InterruptedException {
        int maxSize = 100;
        int count = 1000;
        long start = System.currentTimeMillis();
        TracesSortedSet tracesSortedSet = new TracesSortedSet(maxSize);
        AtomicInteger loops = new AtomicInteger();
        AtomicBoolean exception = new AtomicBoolean();
        AtomicLong ids = new AtomicLong();
        Executors.newSingleThreadExecutor().submit(() -> {
            try {
                while (true) {
                    SortedSet<Traces> copy = tracesSortedSet.getCopy();
                    for (Traces traces : copy) {
                        ids.addAndGet(traces.getTime());
                    }
                    loops.incrementAndGet();
                }
            } catch (Exception e) {
                logger.warn("?", e);
                exception.set(true);
            }
        });

        ExecutorService executorService = Executors.newCachedThreadPool();
        for (int i = 0; i < 5; i++) {
            executorService.submit(() -> addTraces(count, start, tracesSortedSet));
        }

        executorService.shutdown();
        executorService.awaitTermination(1, TimeUnit.MINUTES);
        logger.info("ids {}", ids);
        logger.info("loops {}", loops);
        logger.info("tracesSortedSet {}", tracesSortedSet.size());
        for (Traces traces : tracesSortedSet) {
            assertTrue(traces.getTime() > 970);
        }
        assertEquals(100, tracesSortedSet.size());
        assertTrue(loops.get() > 100);
        assertTrue(ids.get() > 1000 * 1000);
        assertFalse(exception.get());

    }

}
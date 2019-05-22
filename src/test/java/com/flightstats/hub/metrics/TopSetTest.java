package com.flightstats.hub.metrics;

import com.flightstats.hub.config.AppProperties;
import com.flightstats.hub.config.PropertiesLoader;
import com.flightstats.hub.util.Sleeper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
class TopSetTest {

    @Test
    void testSingle() {
        int maxSize = 10;
        int count = 100;
        long start = System.currentTimeMillis();
        TopSortedSet topSortedSet = getTopSortedSet(maxSize);
        addTraces(count, start, topSortedSet);
        assertEquals(maxSize, topSortedSet.size());
        List<Traces> list = new ArrayList<>(topSortedSet);
        for (int i = 0; i < list.size(); i++) {
            assertEquals(count - 1 - i, list.get(i).getTime());
        }
    }

    private TopSortedSet<Traces> getTopSortedSet(int maxSize) {
        return new TopSortedSet<>(maxSize, Traces::getTime, new DescendingTracesComparator());
    }

    private void addTraces(int count, long start, TopSortedSet topSortedSet) {
        for (int i = 0; i < count; i++) {
            Traces traces = new Traces(new AppProperties(PropertiesLoader.getInstance()), i);
            traces.setStart(start - i);
            traces.setEnd(start);
            topSortedSet.add(traces);
            Sleeper.sleep((long) (Math.random()));
        }
    }

    @Test
    void testMulti() throws InterruptedException {
        int maxSize = 100;
        int count = 1000;
        long start = System.currentTimeMillis();
        TopSortedSet<Traces> topSortedSet = getTopSortedSet(maxSize);
        AtomicInteger loops = new AtomicInteger();
        AtomicBoolean exception = new AtomicBoolean();
        AtomicLong ids = new AtomicLong();
        Executors.newSingleThreadExecutor().submit(() -> {
            try {
                while (true) {
                    SortedSet<Traces> copy = topSortedSet.getCopy();
                    for (Traces traces : copy) {
                        ids.addAndGet(traces.getTime());
                    }
                    loops.incrementAndGet();
                }
            } catch (Exception e) {
                log.warn("?", e);
                exception.set(true);
            }
        });

        ExecutorService executorService = Executors.newCachedThreadPool();
        for (int i = 0; i < 5; i++) {
            executorService.submit(() -> addTraces(count, start, topSortedSet));
        }

        executorService.shutdown();
        executorService.awaitTermination(1, TimeUnit.MINUTES);
        log.info("ids {}", ids);
        log.info("loops {}", loops);
        log.info("topSortedSet {}", topSortedSet.size());

        for (Traces traces : topSortedSet) {
            assertTrue(traces.getTime() > 970);
        }
        assertEquals(100, topSortedSet.size());
        assertTrue(loops.get() > 100);
        assertTrue(ids.get() > 1000 * 1000);
        assertFalse(exception.get());
    }

}
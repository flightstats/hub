package com.flightstats.hub.metrics;

import com.flightstats.hub.app.HubServices;
import com.google.common.util.concurrent.AbstractScheduledService;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

@Singleton
public class ContentKeyMap {

    private final static Logger logger = LoggerFactory.getLogger(ContentKeyMap.class);

    private static ConcurrentMap<String, ContentKeyMapStack> active = new ConcurrentHashMap<>();
    private static final TopSortedSet<ContentKeyMapStack> topStacks = new TopSortedSet<>(100,
            ContentKeyMapStack::getCount, (o1, o2) -> (int) (o2.count - o1.count));

    public ContentKeyMap() {
        HubServices.register(new ContentKeyMapService());
    }

    public static void register() {
        String name = Thread.currentThread().getName();
        ContentKeyMapStack stack = active.get(name);
        if (stack == null) {
            stack = new ContentKeyMapStack(name);
            active.put(name, stack);

        } else {
            stack.increment();
        }
    }

    public static List<ContentKeyMapStack> getTop() {
        return new ArrayList<>(topStacks);
    }

    public static List<ContentKeyMapStack> getActive() {
        return new ArrayList<>(active.values());
    }

    private class ContentKeyMapService extends AbstractScheduledService {
        @Override
        protected void runOneIteration() throws Exception {

            long cutoffTime = System.currentTimeMillis() - TimeUnit.MILLISECONDS.convert(1, TimeUnit.MINUTES);
            List<ContentKeyMapStack> removed = new ArrayList<>();

            Collection<ContentKeyMapStack> activeStacks = active.values();
            for (ContentKeyMapStack stack : activeStacks) {
                if (stack.getEnd() < cutoffTime) {
                    removed.add(stack);
                    activeStacks.remove(stack);
                }
            }
            logger.info("removed {} remaining {}", removed.size(), active.size());
            for (ContentKeyMapStack stack : removed) {
                topStacks.add(stack);
            }

            try {
                String output = "";
                for (ContentKeyMapStack stack : topStacks) {
                    output += stack.toString() + "\r\n";
                }
                logger.info("top 100: \r\n" + output);
            } catch (Exception e) {
                logger.warn("unable to output ", e);
            }
        }

        @Override
        protected Scheduler scheduler() {
            return Scheduler.newFixedDelaySchedule(1, 1, TimeUnit.MINUTES);
        }

    }
}

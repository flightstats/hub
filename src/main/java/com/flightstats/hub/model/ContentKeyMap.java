package com.flightstats.hub.model;

import com.flightstats.hub.app.HubServices;
import com.google.common.util.concurrent.AbstractScheduledService;
import com.google.inject.Singleton;
import lombok.EqualsAndHashCode;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

@Singleton
public class ContentKeyMap {

    private final static Logger logger = LoggerFactory.getLogger(ContentKeyMap.class);

    private static final ConcurrentMap<String, ContentKeyMapStack> map = new ConcurrentHashMap<>();

    public static void register() {
        String name = Thread.currentThread().getName();
        ContentKeyMapStack stack = map.get(name);
        if (stack == null) {
            stack = new ContentKeyMapStack();
            map.put(name, stack);
            stack.name = name;
            stack.stacktrace = Thread.currentThread().getStackTrace();
        } else {
            stack.count++;
        }

    }

    @EqualsAndHashCode(of = {"count", "name"})
    private static class ContentKeyMapStack {
        String name;
        long start = System.currentTimeMillis();
        StackTraceElement[] stacktrace;
        long count;
    }

    private class StackComparator implements Comparator<ContentKeyMapStack> {

        @Override
        public int compare(ContentKeyMapStack o1, ContentKeyMapStack o2) {
            return (int) (o2.count - o1.count);
        }
    }

    public ContentKeyMap() {
        HubServices.register(new ContentKeyMapService());
    }

    private class ContentKeyMapService extends AbstractScheduledService {
        @Override
        protected void runOneIteration() throws Exception {
            try {
                TreeSet<ContentKeyMapStack> treeSet = new TreeSet<>(new StackComparator());
                treeSet.addAll(map.values());
                String output = "";
                ArrayList<ContentKeyMapStack> list = new ArrayList(treeSet);
                int items = Math.min(100, list.size());
                for (int i = 0; i < items; i++) {
                    ContentKeyMapStack item = list.get(i);
                    output += item.name + " " + item.count + " " + new DateTime(item.start) + "\r\n";
                }
                logger.info("top 100: \r\n" + output);
            } catch (Exception e) {
                logger.warn("unable to output ", e);
            }
        }

        @Override
        protected Scheduler scheduler() {
            return Scheduler.newFixedDelaySchedule(30, 30, TimeUnit.SECONDS);
        }

    }
}

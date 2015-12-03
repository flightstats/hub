package com.flightstats.hub.metrics;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.util.ObjectRing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

public class ActiveTraces {

    private final static Logger logger = LoggerFactory.getLogger(ActiveTraces.class);

    private static final Map<String, Traces> activeTraces = new ConcurrentHashMap<>();
    private static final ObjectRing<Traces> recent = new ObjectRing(100);
    private static final TracesSortedSet slowest = new TracesSortedSet(100);
    private static final ThreadLocal<Traces> threadLocal = new ThreadLocal();

    public static void start(Object... objects) {
        start(new Traces(objects));
    }

    private static void start(Traces traces) {
        activeTraces.put(traces.getId(), traces);
        setLocal(traces);
        logger.trace("setting {}", traces);
    }

    public static boolean end() {
        Traces traces = threadLocal.get();
        if (null == traces) {
            logger.debug("no Traces found!", new Exception());
            return false;
        } else {
            logger.trace("removing {}", traces.getId());
            activeTraces.remove(traces.getId());
            threadLocal.remove();
            traces.end();
            recent.put(traces);
            slowest.add(traces);
            return true;
        }
    }

    public static void setLocal(Traces traces) {
        threadLocal.set(traces);
    }

    public static Traces getLocal() {
        Traces traces = threadLocal.get();
        if (traces == null) {
            traces = new Traces("error: missing initial context");
            StackTraceElement[] elements = new Exception().getStackTrace();
            for (int i = 0; i < elements.length; i++) {
                traces.add(elements[i].toString());
            }
            start(traces);
        }
        return traces;
    }

    public static void log(ObjectNode root) {
        TreeSet<Traces> orderedActive = new TreeSet<>((t1, t2) -> (int) (t1.getStart() - t2.getStart()));
        orderedActive.addAll(activeTraces.values());
        ArrayNode active = root.putArray("active");
        for (Traces trace : orderedActive) {
            trace.output(active.addObject());
        }
        addItems("slowest", slowest.getCopy(), root);
        addItems("recent", recent.getItems(), root);
    }

    private static void addItems(String fieldName, Collection<Traces> recentItems, ObjectNode root) {
        TreeSet<Traces> orderedRecent = new TreeSet<>(new TracesSortedSet.DescendingTracesComparator());
        orderedRecent.addAll(recentItems);
        ArrayNode recent = root.putArray(fieldName);
        for (Traces trace : orderedRecent) {
            trace.output(recent.addObject());
        }
    }
}

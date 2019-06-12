package com.flightstats.hub.metrics;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.util.ObjectRing;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class ActiveTraces {

    private static final Map<String, Traces> tracesMap = new ConcurrentHashMap<>();
    private static final ThreadLocal<Traces> threadLocal = new ThreadLocal<>();

    //How long an http request needs to take before being considered 'slow' and logged
    private static final int LOG_SLOW_TRACES_IN_SEC = 10 * 1000;
    private static final ObjectRing<Traces> recent = new ObjectRing<>(100);
    private static final TopSortedSet<Traces> slowest = new TopSortedSet<>(100, Traces::getTime, new DescendingTracesComparator());

    public static void start(Object... objects) {
        start(new Traces(objects));
    }

    private static void start(Traces traces) {
        tracesMap.put(traces.getId(), traces);
        setLocal(traces);
        log.trace("setting {}", traces);
    }

    public static boolean end() {
        return end(false, 0);
    }

    public static boolean end(boolean trace, int status) {
        Traces traces = threadLocal.get();
        if (null == traces) {
            log.trace("no Traces found");
            return false;
        } else {
            log.trace("removing {}", traces.getId());
            tracesMap.remove(traces.getId());
            threadLocal.remove();
            traces.end(status);
            traces.log(LOG_SLOW_TRACES_IN_SEC, trace, log);
            recent.put(traces);
            slowest.add(traces);
            return true;
        }
    }

    public static Traces getLocal() {
        Traces traces = threadLocal.get();
        if (traces == null) {
            traces = new Traces("error: missing initial context");
            StackTraceElement[] elements = new Exception().getStackTrace();
            for (StackTraceElement element : elements) {
                traces.add(element.toString());
            }
            start(traces);
        }
        return traces;
    }

    public static void setLocal(Traces traces) {
        threadLocal.set(traces);
    }

    public static void log(ObjectNode root) {
        TreeSet<Traces> orderedActive = new TreeSet<>((t1, t2) -> (int) (t1.getStart() - t2.getStart()));
        orderedActive.addAll(tracesMap.values());
        ArrayNode active = root.putArray("active");
        for (Traces trace : orderedActive) {
            trace.output(active.addObject());
        }
        addItems("slowest", slowest.getCopy(), root);
        addItems("recent", recent.getItems(), root);
    }

    private static void addItems(String fieldName, Collection<Traces> recentItems, ObjectNode root) {
        TreeSet<Traces> orderedRecent = new TreeSet<>(new DescendingTracesComparator());
        orderedRecent.addAll(recentItems);
        ArrayNode recent = root.putArray(fieldName);
        for (Traces trace : orderedRecent) {
            trace.output(recent.addObject());
        }
    }
}

package com.flightstats.hub.metrics;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.model.Traces;
import com.flightstats.hub.model.TracesImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

public class ActiveTraces {

    private final static Logger logger = LoggerFactory.getLogger(ActiveTraces.class);

    private static final Map<String, Traces> activeTraces = new ConcurrentHashMap<>();
    //todo - gfm - 11/17/15 - also track last N traces

    private static ThreadLocal<Traces> threadLocal = new ThreadLocal();

    public static void start(Object... objects) {
        start(new TracesImpl(objects));
    }

    public static void start(Traces traces) {
        activeTraces.put(traces.getId(), traces);
        setLocal(traces);
        logger.trace("setting {}", traces);
    }

    public static void end() {
        Traces traces = threadLocal.get();
        if (null == traces) {
            logger.warn("no Traces found!");
        } else {
            logger.trace("removing {}", traces.getId());
            activeTraces.remove(traces.getId());
            threadLocal.remove();
        }
    }

    public static void setLocal(Traces traces) {
        threadLocal.set(traces);
    }

    public static Traces getLocal() {
        Traces traces = threadLocal.get();
        if (traces == null) {
            traces = new TracesImpl("missing initial context", new Exception());
            start(traces);
        }
        return traces;
    }

    public static void log(ObjectNode root) {
        TreeSet<Traces> ordered = new TreeSet<>((t1, t2) -> (int) (t1.getStart() - t2.getStart()));
        ordered.addAll(activeTraces.values());
        ArrayNode traces = root.putArray("traces");
        for (Traces trace : ordered) {
            trace.output(traces.addObject());
        }
    }
}

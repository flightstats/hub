package com.flightstats.hub.metrics;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.model.Traces;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

public class ActiveTraces {

    private final static Logger logger = LoggerFactory.getLogger(ActiveTraces.class);
    private static final Map<String, Traces> activeTraces = new ConcurrentHashMap<>();

    public static void add(Traces traces) {
        activeTraces.put(traces.getId(), traces);
    }

    public static void remove(Traces traces) {
        activeTraces.remove(traces.getId());
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

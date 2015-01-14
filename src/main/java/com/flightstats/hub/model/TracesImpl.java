package com.flightstats.hub.model;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;

public class TracesImpl implements Traces {

    private long start = System.currentTimeMillis();
    private final List<Trace> traces = Collections.synchronizedList(new ArrayList<>());

    @Override
    public void add(Trace trace) {
        traces.add(trace);
    }

    @Override
    public void add(Object... objects) {
        traces.add(new Trace(objects));
    }

    @Override
    public void add(String string, SortedSet sortedSet) {
        if (sortedSet.isEmpty()) {
            add(string, "empty set");
        } else {
            add(string, sortedSet.size(), sortedSet.first(), sortedSet.last());
        }
    }

    @Override
    public void setStart(long start) {
        this.start = start;
    }

    @Override
    public long getStart() {
        return start;
    }

    @Override
    public void logSlow(long millis, Logger logger) {
        long processingTime = System.currentTimeMillis() - start;
        if (processingTime >= millis) {
            String output = getOutput(logger);
            logger.info("slow processing of {} millis. trace: {}", processingTime, output);
        }
    }

    public void log(Logger logger) {
        String output = getOutput(logger);
        logger.info("trace: {}", output);
    }

    private String getOutput(Logger logger) {
        try {
            traces.add(new Trace("logging"));
            String output = "\n\t";
            synchronized (traces) {
                for (Trace trace : traces) {
                    output += trace.toString() + "\n\t";
                }
            }
            return output;
        } catch (Exception e) {
            logger.warn("unable to log {} traces {}", traces);
            return "unable to output";
        }
    }

    @Override
    public void output(ObjectNode root) {
        ArrayNode traceRoot = root.putArray("trace");
        synchronized (traces) {
            for (Trace trace : traces) {
                traceRoot.add(trace.toString());
            }
        }
    }


}

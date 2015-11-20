package com.flightstats.hub.model;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.joda.time.DateTime;
import org.slf4j.Logger;

import java.util.*;

public class Traces {

    private long start = System.currentTimeMillis();
    private long end;
    private final String id = UUID.randomUUID().toString();
    private final List<Trace> traces = Collections.synchronizedList(new ArrayList<>());

    public Traces() {
    }

    public Traces(Object... objects) {
        add(objects);
    }

    public void end() {
        end = System.currentTimeMillis();
        add("end");
    }

    public void setEnd(long end) {
        this.end = end;
    }

    public long getTime() {
        if (end > 0) {
            return end - start;
        } else {
            return System.currentTimeMillis() - start;
        }
    }

    public void add(Trace trace) {
        traces.add(trace);
    }

    public void add(Object... objects) {
        traces.add(new SingleTrace(objects));
    }

    public void add(String string, SortedSet sortedSet) {
        if (sortedSet.isEmpty()) {
            add(string, "empty set");
        } else {
            add(string, sortedSet.size(), sortedSet.first(), sortedSet.last());
        }
    }

    public void setStart(long start) {
        this.start = start;
    }

    public long getStart() {
        return start;
    }

    public String getId() {
        return id;
    }

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

    public void output(ObjectNode root) {
        root.put("id", id);
        root.put("start", new DateTime(start).toString());
        root.put("millis", getTime());
        ArrayNode traceRoot = root.putArray("trace");
        synchronized (traces) {
            for (Trace trace : traces) {
                traceRoot.add(trace.toString());
            }
        }
    }


}

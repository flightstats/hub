package com.flightstats.hub.model;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;

import java.util.SortedSet;

public class NoOpTraces implements Traces {
    @Override
    public void add(Trace trace) {

    }

    @Override
    public void add(Object... objects) {

    }

    @Override
    public void add(SortedSet sortedSet, Object... objects) {

    }

    @Override
    public void setStart(long start) {

    }

    @Override
    public void logSlow(long millis, Logger logger) {

    }

    @Override
    public void output(ObjectNode root) {

    }
}

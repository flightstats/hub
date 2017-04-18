package com.flightstats.hub.model;

import org.joda.time.DateTime;

import java.util.Arrays;

public class SingleTrace implements Trace {
    private final Object[] objects;
    private final long time = System.currentTimeMillis();

    public SingleTrace(Object... objects) {
        this.objects = objects;
    }

    @Override
    public String toString() {
        return new DateTime(time) + " " + Arrays.toString(objects);
    }

    @Override
    public String context() {
        return Arrays.toString(objects);
    }

    public Object[] getObjects() {
        return this.objects;
    }

    public long getTime() {
        return this.time;
    }
}

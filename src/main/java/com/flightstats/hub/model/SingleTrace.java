package com.flightstats.hub.model;

import lombok.Getter;
import org.joda.time.DateTime;

import java.util.Arrays;

@Getter
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
}

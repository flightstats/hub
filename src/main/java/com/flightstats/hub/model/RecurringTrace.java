package com.flightstats.hub.model;

import lombok.Getter;
import org.joda.time.DateTime;

import java.util.Arrays;

@Getter
public class RecurringTrace implements Trace {
    private final long startTime = System.currentTimeMillis();
    private Object[] objects;
    private long count = 0;
    private long latestTime = System.currentTimeMillis();

    public RecurringTrace(Object... objects) {
        this.objects = objects;
    }

    public void update(Object... objects) {
        latestTime = System.currentTimeMillis();
        this.objects = objects;
        count++;
    }

    @Override
    public String toString() {
        return count +
                " times from " + new DateTime(startTime) +
                " until " + new DateTime(latestTime) +
                " " + Arrays.toString(objects);
    }
}

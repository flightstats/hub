package com.flightstats.hub.model;

import com.flightstats.hub.util.TimeUtil;
import lombok.Getter;
import org.joda.time.DateTime;

import java.util.Arrays;

@Getter
public class Trace {
    private final Object[] objects;
    private final DateTime time;

    public Trace(Object... objects) {
        this.objects = objects;
        this.time = TimeUtil.now();
    }

    @Override
    public String toString() {
        return time + " " + Arrays.toString(objects);
    }
}

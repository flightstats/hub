package com.flightstats.hub.metrics;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.joda.time.DateTime;

@EqualsAndHashCode(of = {"count", "name"})
@Getter
public class ContentKeyMapStack {
    String name;
    long count;
    long start = System.currentTimeMillis();
    private long end = System.currentTimeMillis();
    private StackTraceElement[] stacktrace = Thread.currentThread().getStackTrace();

    public ContentKeyMapStack(String name) {
        this.name = name;
    }

    public void increment() {
        count++;
        end = System.currentTimeMillis();
    }

    @Override
    public String toString() {
        return "count=" + count +
                ", name='" + name + '\'' +
                ", start=" + new DateTime(start) +
                ", end=" + new DateTime(end);
    }
}

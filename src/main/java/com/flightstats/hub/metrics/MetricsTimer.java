package com.flightstats.hub.metrics;

import com.google.inject.Inject;
import com.timgroup.statsd.StatsDClient;

import java.util.concurrent.Callable;

public class MetricsTimer implements MetricsSender {
    private final MetricsSender sender;
    private final static StatsDClient statsd = DataDog.statsd;

    @Inject
    public MetricsTimer(MetricsSender sender) {
        this.sender = sender;
    }

    public <T> T time(String type, String name, Callable<T> callable) throws Exception {
        long start = System.currentTimeMillis();
        try {
            return callable.call();
        } finally {
            long time = System.currentTimeMillis() - start;
            statsd.time(type, time, type + ":" + name);
            sender.send(name, time);
        }
    }

    @Override
    public void send(String name, Object value) {
        // BC - is count correct, or do we want gauge?
        statsd.count("timer", (Long) value, "name:" + name);
        sender.send(name, value);
    }
}

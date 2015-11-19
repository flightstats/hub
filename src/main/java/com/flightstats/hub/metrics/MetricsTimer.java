package com.flightstats.hub.metrics;

import com.google.inject.Inject;

import java.util.concurrent.Callable;

public class MetricsTimer implements MetricsSender {
    private final MetricsSender sender;

    @Inject
    public MetricsTimer(MetricsSender sender) {
        this.sender = sender;
    }

    public <T> T time(String name, Callable<T> callable) throws Exception {
        long start = System.currentTimeMillis();
        try {
            return callable.call();
        } finally {
            sender.send(name, System.currentTimeMillis() - start);
        }
    }

    @Override
    public void send(String name, Object value) {
        sender.send(name, value);
    }
}

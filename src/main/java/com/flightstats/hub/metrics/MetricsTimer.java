package com.flightstats.hub.metrics;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.inject.Inject;

import java.util.concurrent.Callable;

public class MetricsTimer {
    private final MetricRegistry registry;
    private final HostedGraphiteSender sender;

    @Inject
    public MetricsTimer(MetricRegistry registry, HostedGraphiteSender sender) {
        this.registry = registry;
        this.sender = sender;
    }

    public <T> T time(String name, Callable<T> callable) throws Exception {
        return time(name, name, callable);
    }

    public <T> T time(String oldName, String newName, Callable<T> callable) throws Exception {
        Timer timer = registry.timer(oldName);
        Timer.Context context = timer.time();
        long start = System.currentTimeMillis();
        try {
            return callable.call();
        } finally {
            context.stop();
            sender.send(newName, System.currentTimeMillis() - start);
        }
    }
}

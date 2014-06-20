package com.flightstats.hub.metrics;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.inject.Inject;

import java.util.concurrent.Callable;

public class MetricsTimer {
    private final MetricRegistry registry;

    @Inject
    public MetricsTimer(MetricRegistry registry) {
        this.registry = registry;
    }

    public <T> T time(String name, Callable<T> callable) throws Exception {
        Timer timer = registry.timer(name);
        Timer.Context context = timer.time();
        try {
            return callable.call();
        } finally {
            context.stop();
        }
    }
}

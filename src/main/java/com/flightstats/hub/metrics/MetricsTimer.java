package com.flightstats.hub.metrics;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.inject.Inject;

/**
 *
 */
public class MetricsTimer {
    private final MetricRegistry registry;

    @Inject
    public MetricsTimer(MetricRegistry registry) {
        this.registry = registry;
    }

    public <T> T time(String name, TimedCallback<T> timedCallback) {
        Timer timer = registry.timer(name);
        Timer.Context context = timer.time();
        try {
            return timedCallback.call();
        } finally {
            context.stop();
        }
    }
}

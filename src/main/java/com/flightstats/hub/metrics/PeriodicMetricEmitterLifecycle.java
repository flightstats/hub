package com.flightstats.hub.metrics;

import com.google.common.util.concurrent.AbstractScheduledService;
import javax.inject.Inject;

import java.util.concurrent.TimeUnit;

public class PeriodicMetricEmitterLifecycle extends AbstractScheduledService{
    private final PeriodicMetricEmitter periodicMetricEmitter;

    @Inject
    public PeriodicMetricEmitterLifecycle(PeriodicMetricEmitter periodicMetricEmitter) {
        this.periodicMetricEmitter = periodicMetricEmitter;
    }
    @Override
    protected void runOneIteration() {
        periodicMetricEmitter.emit();
    }

    @Override
    protected Scheduler scheduler() {
        return Scheduler.newFixedRateSchedule(0, 1, TimeUnit.MINUTES);
    }
}

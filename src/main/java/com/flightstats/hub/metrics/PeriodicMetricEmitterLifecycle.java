package com.flightstats.hub.metrics;

import com.google.common.util.concurrent.AbstractScheduledService;
import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Named;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

@Slf4j
public class PeriodicMetricEmitterLifecycle extends AbstractScheduledService{
    private Collection<PeriodicMetricEmitter> emitters;

    @Inject
    public PeriodicMetricEmitterLifecycle(@Named("PeriodicMetricEmitters") Collection<PeriodicMetricEmitter> emitters) {
        this.emitters = emitters;
    }
    @Override
    protected void runOneIteration() {
        emitters.forEach((emitter) -> emitter.emit());
    }

    @Override
    protected Scheduler scheduler() {
        return Scheduler.newFixedRateSchedule(0, 1, TimeUnit.MINUTES);
    }
}

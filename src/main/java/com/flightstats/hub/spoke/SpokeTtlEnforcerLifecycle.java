package com.flightstats.hub.spoke;

import com.google.common.util.concurrent.AbstractScheduledService;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

@Slf4j
public class SpokeTtlEnforcerLifecycle extends AbstractScheduledService {

    private SpokeTtlEnforcer spokeTtlEnforcer;

    private SpokeStore spokeStore;

    public SpokeTtlEnforcerLifecycle(SpokeStore spokeStore, SpokeTtlEnforcer spokeTtlEnforcer) {
        this.spokeStore = spokeStore;
        this.spokeTtlEnforcer = spokeTtlEnforcer;
    }

    @Override
    protected void runOneIteration() {
        this.spokeTtlEnforcer.cleanup(spokeStore);
    }

    @Override
    protected Scheduler scheduler() {
        return Scheduler.newFixedRateSchedule(1, 1, TimeUnit.MINUTES);
    }

}
package com.flightstats.hub.spoke;

import com.google.common.util.concurrent.AbstractScheduledService;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

@Slf4j
public class SpokeTtlEnforcerService extends AbstractScheduledService {

    @Inject
    private SpokeTtlEnforcer spokeTtlEnforcer;

    private SpokeStore spokeStore;

    public SpokeTtlEnforcerService(SpokeStore spokeStore) {
        this.spokeStore = spokeStore;
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
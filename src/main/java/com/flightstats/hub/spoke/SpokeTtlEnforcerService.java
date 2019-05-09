package com.flightstats.hub.spoke;

import com.google.common.util.concurrent.AbstractScheduledService;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

@Slf4j
public class SpokeTtlEnforcerService extends AbstractScheduledService {

    private SpokeTtlEnforcer spokeTtlEnforcer;

    private SpokeStore spokeStore;

    public SpokeTtlEnforcerService(SpokeStore spokeStore, SpokeTtlEnforcer spokeTtlEnforcer) {
        this.spokeStore = spokeStore;
        this.spokeTtlEnforcer = spokeTtlEnforcer;
    }

    @Override
    protected void runOneIteration() {
        log.info("*****************************************SpokeTtlEnforcerService  cleaning up start*****************");
        this.spokeTtlEnforcer.cleanup(spokeStore);
        log.info("*****************************************SpokeTtlEnforcerService  cleaning up done*****************");
    }

    @Override
    protected Scheduler scheduler() {
        return Scheduler.newFixedRateSchedule(1, 1, TimeUnit.MINUTES);
    }

}
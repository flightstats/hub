package com.flightstats.hub.spoke;

import com.flightstats.hub.app.HubServices;
import com.flightstats.hub.config.AppProperty;
import com.google.common.util.concurrent.AbstractScheduledService;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

@Singleton
@Slf4j
public class GCRunner {

    private final int gcMinutes;

    @Inject
    public GCRunner(AppProperty appProperty) {
        this.gcMinutes = appProperty.getGCSchedulerDelayInMinutes();
        if (appProperty.isRunGC()) {
            HubServices.register(new GCRunnerService());
        }
    }

    private void run() {
        log.info("running GC");
        long start = System.currentTimeMillis();
        System.gc();
        log.info("ran GC {}", (System.currentTimeMillis() - start));
    }

    private class GCRunnerService extends AbstractScheduledService {
        @Override
        protected void runOneIteration() {
            run();
        }

        @Override
        protected Scheduler scheduler() {
            return Scheduler.newFixedDelaySchedule(gcMinutes, gcMinutes, TimeUnit.MINUTES);
        }
    }

}

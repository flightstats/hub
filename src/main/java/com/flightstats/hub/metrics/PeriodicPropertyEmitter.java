package com.flightstats.hub.metrics;

import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.app.HubServices;
import com.flightstats.hub.spoke.SpokeStore;
import com.google.common.util.concurrent.AbstractScheduledService;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

public class PeriodicPropertyEmitter {

    @Inject
    private MetricsService metricsService;

    PeriodicPropertyEmitter() {
        HubServices.register(new PeriodicPropertyEmitterService(), HubServices.TYPE.AFTER_HEALTHY_START);
    }

    private class PeriodicPropertyEmitterService extends AbstractScheduledService {

        @Override
        protected void runOneIteration() {
            metricsService.gauge("s3.writeQueue.total", HubProperties.getS3WriteQueueSize());
            metricsService.gauge("s3.writeQueue.threads", HubProperties.getS3WriteQueueThreads());
            metricsService.gauge("spoke.write.ttl", HubProperties.getSpokeTtlMinutes(SpokeStore.WRITE));
            metricsService.gauge("spoke.read.ttl", HubProperties.getSpokeTtlMinutes(SpokeStore.READ));
        }

        @Override
        protected Scheduler scheduler() {
            return Scheduler.newFixedRateSchedule(0, 1, TimeUnit.MINUTES);
        }
    }
}

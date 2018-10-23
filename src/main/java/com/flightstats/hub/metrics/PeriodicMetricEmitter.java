package com.flightstats.hub.metrics;

import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.app.HubServices;
import com.flightstats.hub.dao.aws.S3Verifier;
import com.flightstats.hub.spoke.SpokeStore;
import com.google.common.util.concurrent.AbstractScheduledService;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

public class PeriodicMetricEmitter {

    private final MetricsService metricsService;
    private final HubProperties hubProperties;

    @Inject
    PeriodicMetricEmitter(MetricsService metricsService, HubProperties hubProperties) {
        this.metricsService = metricsService;
        this.hubProperties = hubProperties;

        HubServices.register(new PeriodicMetricEmitterService(), HubServices.TYPE.AFTER_HEALTHY_START);
    }

    private class PeriodicMetricEmitterService extends AbstractScheduledService {

        @Override
        protected void runOneIteration() {
            metricsService.gauge("s3.writeQueue.total", hubProperties.getS3WriteQueueSize());
            metricsService.gauge("s3.writeQueue.threads", hubProperties.getS3WriteQueueThreads());
            metricsService.gauge("spoke.write.ttl", hubProperties.getSpokeTtlMinutes(SpokeStore.WRITE));
            metricsService.gauge("spoke.read.ttl", hubProperties.getSpokeTtlMinutes(SpokeStore.READ));
            metricsService.count(S3Verifier.MISSING_ITEM_METRIC_NAME, 0);
        }

        @Override
        protected Scheduler scheduler() {
            return Scheduler.newFixedRateSchedule(0, 1, TimeUnit.MINUTES);
        }
    }
}

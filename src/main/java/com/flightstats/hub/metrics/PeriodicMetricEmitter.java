package com.flightstats.hub.metrics;

import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.app.HubServices;
import com.flightstats.hub.dao.aws.S3Verifier;
import com.flightstats.hub.spoke.SpokeStore;
import com.google.common.util.concurrent.AbstractScheduledService;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

public class PeriodicMetricEmitter {
    private StatsDHandlers statsDHandlers;

    @Inject
    PeriodicMetricEmitter(StatsDHandlers statsDHandlers) {
        this.statsDHandlers = statsDHandlers;
    }

    void emit() {
        statsDHandlers.gauge("s3.writeQueue.total", HubProperties.getS3WriteQueueSize());
        statsDHandlers.gauge("s3.writeQueue.threads", HubProperties.getS3WriteQueueThreads());
        statsDHandlers.gauge("spoke.write.ttl", HubProperties.getSpokeTtlMinutes(SpokeStore.WRITE));
        statsDHandlers.gauge("spoke.read.ttl", HubProperties.getSpokeTtlMinutes(SpokeStore.READ));
        statsDHandlers.count(S3Verifier.MISSING_ITEM_METRIC_NAME, 0);
    }
}

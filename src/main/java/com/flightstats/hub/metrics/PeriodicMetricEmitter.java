package com.flightstats.hub.metrics;

import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.spoke.SpokeStore;

import javax.inject.Inject;

public class PeriodicMetricEmitter {
    private StatsdReporter statsdReporter;

    @Inject
    PeriodicMetricEmitter(StatsdReporter statsdReporter) {
        this.statsdReporter = statsdReporter;
    }

    void emit() {
        statsdReporter.gauge("s3.writeQueue.total", HubProperties.getS3WriteQueueSize());
        statsdReporter.gauge("s3.writeQueue.threads", HubProperties.getS3WriteQueueThreads());
        statsdReporter.gauge("spoke.write.ttl", HubProperties.getSpokeTtlMinutes(SpokeStore.WRITE));
        statsdReporter.gauge("spoke.read.ttl", HubProperties.getSpokeTtlMinutes(SpokeStore.READ));
    }
}

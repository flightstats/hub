package com.flightstats.hub.metrics;

import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.dao.aws.S3AccessMonitor;
import com.flightstats.hub.spoke.SpokeStore;

import javax.inject.Inject;

public class PeriodicMetricEmitter {
    private StatsdReporter statsdReporter;
    private S3AccessMonitor s3AccessMonitor;

    @Inject
    PeriodicMetricEmitter(StatsdReporter statsdReporter, S3AccessMonitor s3AccessMonitor) {
        this.statsdReporter = statsdReporter;
        this.s3AccessMonitor = s3AccessMonitor;
    }

    void emit() {
        statsdReporter.gauge("s3.writeQueue.total", HubProperties.getS3WriteQueueSize());
        statsdReporter.gauge("s3.writeQueue.threads", HubProperties.getS3WriteQueueThreads());
        statsdReporter.gauge("spoke.write.ttl", HubProperties.getSpokeTtlMinutes(SpokeStore.WRITE));
        statsdReporter.gauge("spoke.read.ttl", HubProperties.getSpokeTtlMinutes(SpokeStore.READ));
        if (!s3AccessMonitor.verifyReadWriteAccess()) {
            statsdReporter.incrementCounter("s3.access.failed");
        }
    }
}

package com.flightstats.hub.metrics;

import com.flightstats.hub.config.S3Property;
import com.flightstats.hub.config.SpokeProperty;
import com.flightstats.hub.dao.aws.S3AccessMonitor;
import com.flightstats.hub.spoke.SpokeStore;

import javax.inject.Inject;

public class PeriodicMetricEmitter {
    private StatsdReporter statsdReporter;
    private S3AccessMonitor s3AccessMonitor;
    private S3Property s3Property;
    private SpokeProperty spokeProperty;

    @Inject
    PeriodicMetricEmitter(StatsdReporter statsdReporter,
                          S3AccessMonitor s3AccessMonitor,
                          S3Property s3Property,
                          SpokeProperty spokeProperty) {
        this.statsdReporter = statsdReporter;
        this.s3AccessMonitor = s3AccessMonitor;
        this.s3Property = s3Property;
        this.spokeProperty = spokeProperty;
    }

    void emit() {
        statsdReporter.gauge("s3.writeQueue.total", s3Property.getWriteQueueSize());
        statsdReporter.gauge("s3.writeQueue.threads", s3Property.getWriteQueueThreadCount());
        statsdReporter.gauge("spoke.write.ttl", spokeProperty.getTtlMinutes(SpokeStore.WRITE));
        statsdReporter.gauge("spoke.read.ttl", spokeProperty.getTtlMinutes(SpokeStore.READ));
        if (!s3AccessMonitor.verifyReadWriteAccess()) {
            statsdReporter.incrementCounter("s3.access.failed");
        }
    }
}

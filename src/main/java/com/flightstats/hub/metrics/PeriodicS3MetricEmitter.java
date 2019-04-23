package com.flightstats.hub.metrics;

import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.dao.aws.S3AccessMonitor;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class PeriodicS3MetricEmitter implements PeriodicMetricEmitter {
    private final S3AccessMonitor s3AccessMonitor;
    private final StatsdReporter statsdReporter;

    @Inject
    PeriodicS3MetricEmitter(StatsdReporter statsdReporter, S3AccessMonitor s3AccessMonitor) {
        this.statsdReporter = statsdReporter;
        this.s3AccessMonitor = s3AccessMonitor;
    }

    @Override
    public void emit() {
        statsdReporter.gauge("s3.writeQueue.total", HubProperties.getS3WriteQueueSize());
        statsdReporter.gauge("s3.writeQueue.threads", HubProperties.getS3WriteQueueThreads());
        if (!s3AccessMonitor.verifyReadWriteAccess()) {
            statsdReporter.incrementCounter("s3.access.failed");
        }
    }

}

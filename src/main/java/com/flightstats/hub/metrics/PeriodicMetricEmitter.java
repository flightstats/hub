package com.flightstats.hub.metrics;

import com.flightstats.hub.config.properties.S3Properties;
import com.flightstats.hub.config.properties.SpokeProperties;
import com.flightstats.hub.dao.aws.S3AccessMonitor;
import com.flightstats.hub.spoke.SpokeStore;

import javax.inject.Inject;

public class PeriodicMetricEmitter {
    private final StatsdReporter statsdReporter;
    private final S3AccessMonitor s3AccessMonitor;
    private final S3Properties s3Properties;
    private final SpokeProperties spokeProperties;

    @Inject
    PeriodicMetricEmitter(StatsdReporter statsdReporter,
                          S3AccessMonitor s3AccessMonitor,
                          S3Properties s3Properties,
                          SpokeProperties spokeProperties) {
        this.statsdReporter = statsdReporter;
        this.s3AccessMonitor = s3AccessMonitor;
        this.s3Properties = s3Properties;
        this.spokeProperties = spokeProperties;
    }



    void emit() {
        statsdReporter.gauge("s3.writeQueue.total", s3Properties.getWriteQueueSize());
        statsdReporter.gauge("s3.writeQueue.threads", s3Properties.getWriteQueueThreadCount());
        statsdReporter.gauge("spoke.write.ttl", spokeProperties.getTtlMinutes(SpokeStore.WRITE));
        statsdReporter.gauge("spoke.read.ttl", spokeProperties.getTtlMinutes(SpokeStore.READ));
        if (!s3AccessMonitor.verifyReadWriteAccess()) {
            statsdReporter.incrementCounter("s3.access.failed");
        }
        statsdReporter.serviceCheck()
    }
}

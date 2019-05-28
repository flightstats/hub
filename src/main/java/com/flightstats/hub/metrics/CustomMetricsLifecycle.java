package com.flightstats.hub.metrics;

import com.flightstats.hub.config.properties.MetricsProperties;
import com.google.common.util.concurrent.AbstractScheduledService;
import com.google.inject.Inject;

import java.util.concurrent.TimeUnit;

public class CustomMetricsLifecycle extends AbstractScheduledService {
    private final CustomMetricsReporter collector;
    private final int reportingIntervalInSeconds;

    @Inject
    public CustomMetricsLifecycle(
            CustomMetricsReporter collector,
            MetricsProperties metricsProperties) {
        this.collector = collector;
        this.reportingIntervalInSeconds = metricsProperties.getReportingIntervalInSeconds();
    }

    @Override
    protected void runOneIteration() {
        collector.run();
    }

    @Override
    protected AbstractScheduledService.Scheduler scheduler() {
        return AbstractScheduledService.Scheduler.newFixedDelaySchedule(0, reportingIntervalInSeconds, TimeUnit.SECONDS);
    }
}

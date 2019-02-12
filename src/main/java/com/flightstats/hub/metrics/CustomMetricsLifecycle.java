package com.flightstats.hub.metrics;

import com.google.common.util.concurrent.AbstractScheduledService;
import java.util.concurrent.TimeUnit;

public class CustomMetricsLifecycle extends AbstractScheduledService{
    private CustomMetricsReporter collector;
    private MetricsConfig metricsConfig;

    public CustomMetricsLifecycle(
            CustomMetricsReporter collector,
            MetricsConfig metricsConfig) {
        this.collector = collector;
        this.metricsConfig = metricsConfig;
    }

    @Override
    protected void runOneIteration() throws Exception {
         collector.run();
    }

    @Override
    protected AbstractScheduledService.Scheduler scheduler() {
        return AbstractScheduledService.Scheduler.newFixedDelaySchedule(0, metricsConfig.getReportingIntervalSeconds(), TimeUnit.SECONDS);
    }
}

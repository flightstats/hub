package com.flightstats.hub.metrics;

import com.codahale.metrics.ScheduledReporter;
import com.flightstats.hub.config.properties.MetricsProperties;
import com.flightstats.hub.config.properties.TickMetricsProperties;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

@Singleton
@Slf4j
public class InfluxdbReporterLifecycle extends AbstractIdleService {
    private final ScheduledReporter influxdbReporter;
    private final MetricsProperties metricsProperties;
    private final TickMetricsProperties tickMetricsProperties;

    @Inject
    public InfluxdbReporterLifecycle(
            ScheduledReporter influxdbReporter,
            TickMetricsProperties tickMetricsProperties,
            MetricsProperties metricsProperties) {
        this.influxdbReporter = influxdbReporter;
        this.tickMetricsProperties = tickMetricsProperties;
        this.metricsProperties = metricsProperties;
    }

    public void startUp() {
        if (metricsProperties.isEnabled()) {
            log.info(
                    "starting metrics reporting to influxdb at {}://{}:{}",
                    tickMetricsProperties.getInfluxDbProtocol(),
                    tickMetricsProperties.getInfluxDbHost(),
                    tickMetricsProperties.getInfluxDbPort());
            int intervalSeconds = metricsProperties.getReportingIntervalInSeconds();
            influxdbReporter.start(intervalSeconds, TimeUnit.SECONDS);
        } else {
            log.info("not starting influxdb reporter: disabled");
        }

    }

    public void shutDown() {
        log.info("shutting down influxdb reporter");
        if (isRunning()) {
            influxdbReporter.stop();
        }
    }
}



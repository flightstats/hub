package com.flightstats.hub.metrics;

import com.codahale.metrics.ScheduledReporter;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

@Singleton
@Slf4j
public class InfluxdbReporterLifecycle extends AbstractIdleService {
    private final ScheduledReporter influxdbReporter;
    private final MetricsConfig metricsConfig;

    @Inject
    public InfluxdbReporterLifecycle(
            ScheduledReporter influxdbReporter,
            MetricsConfig metricsConfig
            ) {
        this.influxdbReporter = influxdbReporter;
        this.metricsConfig = metricsConfig;
    }

    public void startUp() {
        if (metricsConfig.isEnabled()) {
            log.info(
                    "starting metrics reporting to influxdb at {}://{}:{}",
                    metricsConfig.getInfluxdbProtocol(),
                    metricsConfig.getInfluxdbHost(),
                    metricsConfig.getInfluxdbPort());
            int intervalSeconds = metricsConfig.getReportingIntervalSeconds();
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



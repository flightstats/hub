package com.flightstats.hub.metrics;

import com.codahale.metrics.ScheduledReporter;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import java.util.concurrent.TimeUnit;

@Singleton
public class InfluxdbReporterLifecycle extends AbstractIdleService {
    private final Logger logger = LoggerFactory.getLogger(InfluxdbReporterLifecycle.class);
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
            logger.info(
                    "starting metrics reporting to influxdb at {}://{}:{}",
                    metricsConfig.getInfluxdbProtocol(),
                    metricsConfig.getInfluxdbHost(),
                    metricsConfig.getInfluxdbPort());
            int intervalSeconds = metricsConfig.getReportingIntervalSeconds();
            influxdbReporter.start(intervalSeconds, TimeUnit.SECONDS);

        } else {
            logger.info("not starting influxdb reporter: disabled");
        }

    }

    public void shutDown() {
        logger.info("shutting down influxdb reporter");
        if (isRunning()) {
            influxdbReporter.stop();
        }
    }
}



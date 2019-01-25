package com.flightstats.hub.metrics;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.jvm.GarbageCollectorMetricSet;
import com.codahale.metrics.jvm.CachedThreadStatesGaugeSet;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;
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
    public final static String NAME = "InfluxdbReporter";
    private final MetricRegistry metricsRegistry;
    private final ScheduledReporter influxdbReporter;
    private final MetricsConfig metricsConfig;

    @Inject
    public InfluxdbReporterLifecycle(
            MetricRegistry metricsRegistry,
            ScheduledReporter influxdbReporter,
            MetricsConfig metricsConfig
            ) {
        this.metricsRegistry = metricsRegistry;
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
            metricsRegistry.registerAll(new GarbageCollectorMetricSet());
            metricsRegistry.registerAll(new CachedThreadStatesGaugeSet(intervalSeconds, TimeUnit.SECONDS));
            metricsRegistry.registerAll(new MemoryUsageGaugeSet());
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



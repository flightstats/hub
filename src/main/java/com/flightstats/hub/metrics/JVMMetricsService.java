package com.flightstats.hub.metrics;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.jvm.GarbageCollectorMetricSet;
import com.codahale.metrics.jvm.CachedThreadStatesGaugeSet;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;
import com.codahale.metrics.ScheduledReporter;
import com.google.inject.Singleton;
import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import java.util.SortedSet;
import java.util.concurrent.TimeUnit;

@Singleton
public class JVMMetricsService {
    private final Logger logger = LoggerFactory.getLogger(JVMMetricsService.class);

    @Inject MetricRegistry registry;
    @Inject MetricsConfigProvider configProvider;
    @Inject ScheduledReporter influxdbReporter;

    @Inject
    JVMMetricsService(MetricRegistry registry, ScheduledReporter influxdbReporter, MetricsConfigProvider configProvider) {
        this.registry = registry;
        this.influxdbReporter = influxdbReporter;
        this.configProvider = configProvider;
        if (configProvider.enabled()) {
            logger.info(
                    "starting jvm metrics service reporting to {} :// {} : {}",
                    configProvider.getInfluxdbProtocol(),
                    configProvider.getInfluxdbHost(),
                    configProvider.getInfluxdbPort());
            int intervalSeconds = configProvider.getReportingIntervalSeconds();
            String metricPrefix = "jvm_";
            registry.register(metricPrefix + "gc", new GarbageCollectorMetricSet());
            registry.register(metricPrefix + "thread", new CachedThreadStatesGaugeSet(intervalSeconds, TimeUnit.SECONDS));
            registry.register(metricPrefix + "memory", new MemoryUsageGaugeSet());
            influxdbReporter.start(intervalSeconds, TimeUnit.SECONDS);

        } else {
            logger.info("not starting metrics collection for jvm: disabled");
        }
    }

    @VisibleForTesting SortedSet<String>  getRegisteredMetricNames() {
        return registry.getNames();
    }

    @VisibleForTesting void stop() {
        influxdbReporter.stop();
    }
}



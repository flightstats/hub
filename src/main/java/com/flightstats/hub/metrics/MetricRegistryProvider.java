package com.flightstats.hub.metrics;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.jvm.CachedThreadStatesGaugeSet;
import com.codahale.metrics.jvm.GarbageCollectorMetricSet;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

import java.util.concurrent.TimeUnit;

@Singleton
public class MetricRegistryProvider implements Provider<MetricRegistry> {
    private MetricsConfig metricsConfig;

    @Inject
    public MetricRegistryProvider(MetricsConfig metricsConfig) {
        this.metricsConfig = metricsConfig;
    }

    public MetricRegistry get() {
        MetricRegistry metricsRegistry = new MetricRegistry();
        int intervalSeconds = metricsConfig.getReportingIntervalSeconds();
        metricsRegistry.register("gc", new GarbageCollectorMetricSet());
        metricsRegistry.register("thread", new CachedThreadStatesGaugeSet(intervalSeconds, TimeUnit.SECONDS));
        metricsRegistry.register("memory", new MemoryUsageGaugeSet());
        return metricsRegistry;
    }
}

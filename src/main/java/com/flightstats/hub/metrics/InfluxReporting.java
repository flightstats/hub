package com.flightstats.hub.metrics;

import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.jvm.GarbageCollectorMetricSet;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;
import com.codahale.metrics.jvm.ThreadStatesGaugeSet;
import com.conducivetech.services.common.util.Haltable;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.flightstats.jerseyguice.metrics.KeyPrefixedMetricSet.prefix;

public class InfluxReporting implements Haltable {
    private static final Logger logger = LoggerFactory.getLogger(InfluxReporting.class);

    private final List<InfluxdbReporter> reporters = new LinkedList<>();

    @Inject
    public InfluxReporting(MetricRegistry registry) throws Exception {
        logger.info("starting");
        registerJvmMetrics(registry);
        //todo - gfm - 8/14/14 - create a filter to remove any metrics with test in the name

        Influxdb influxdb = new Influxdb("104.131.142.112", 8086, "hub_local", "greg", "123456", TimeUnit.MILLISECONDS);
        InfluxdbReporter reporter = InfluxdbReporter
                .forRegistry(registry)
                .prefixedWith("hub")
                .convertRatesTo(TimeUnit.MINUTES)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .filter(MetricFilter.ALL)
                .build(influxdb);
        reporter.start(5, TimeUnit.SECONDS);
        reporters.add(reporter);

    }

    private void registerJvmMetrics(MetricRegistry registry) {
        registry.registerAll(prefix("jvm.gc", new GarbageCollectorMetricSet()));
        registry.registerAll(prefix("jvm.memory", new MemoryUsageGaugeSet()));
        registry.registerAll(prefix("jvm.threads", new ThreadStatesGaugeSet()));
    }

    @Override
    public void halt() {
        for (InfluxdbReporter reporter : reporters)
            reporter.stop();
    }
}

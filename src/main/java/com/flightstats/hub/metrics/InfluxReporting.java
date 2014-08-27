package com.flightstats.hub.metrics;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.jvm.GarbageCollectorMetricSet;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;
import com.codahale.metrics.jvm.ThreadStatesGaugeSet;
import com.conducivetech.services.common.util.Haltable;
import com.google.inject.Inject;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.flightstats.jerseyguice.metrics.KeyPrefixedMetricSet.prefix;

public class InfluxReporting implements Haltable {
    private static final Logger logger = LoggerFactory.getLogger(InfluxReporting.class);

    private final List<InfluxReporter> reporters = new LinkedList<>();

    @Inject
    public InfluxReporting(MetricRegistry registry) throws Exception {
        logger.info("starting");
        registerJvmMetrics(registry);
        //Influxdb influxdb = new Influxdb("104.131.142.112", 8086, "hub_local", "greg", "123456", TimeUnit.MILLISECONDS);
        InfluxDB influxDB = InfluxDBFactory.connect("http://104.131.142.112:8086", "greg", "123456");
        //InfluxDB influxDB = InfluxDBFactory.connect("http://127.0.0.1:8086", "test", "test");

        InfluxConfig config = InfluxConfig.builder()
                .registry(registry)
                .prefix("hub")
                .influxDB(influxDB)
                .databaseName("hub_local")
                .rateUnit(TimeUnit.SECONDS)
                .durationUnit(TimeUnit.MILLISECONDS)
                .filter(new HubMetricsFilter())
                .build();
        InfluxReporter reporter = new InfluxReporter(config);
        reporter.start(30, TimeUnit.SECONDS);
        reporters.add(reporter);
    }

    private void registerJvmMetrics(MetricRegistry registry) {
        registry.registerAll(prefix("jvm.gc", new GarbageCollectorMetricSet()));
        registry.registerAll(prefix("jvm.memory", new MemoryUsageGaugeSet()));
        registry.registerAll(prefix("jvm.threads", new ThreadStatesGaugeSet()));
    }

    @Override
    public void halt() {
        for (InfluxReporter reporter : reporters)
            reporter.stop();
    }
}

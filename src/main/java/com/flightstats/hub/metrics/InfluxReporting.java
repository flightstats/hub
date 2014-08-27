package com.flightstats.hub.metrics;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.jvm.GarbageCollectorMetricSet;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;
import com.codahale.metrics.jvm.ThreadStatesGaugeSet;
import com.conducivetech.services.common.util.Haltable;
import com.google.inject.Inject;
import com.google.inject.name.Named;
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
    public InfluxReporting(MetricRegistry registry,
                           @Named("influx.enable") boolean enable,
                           @Named("influx.registerJvmMetrics") boolean registerJvm,
                           @Named("influx.hosts") String hosts,
                           @Named("influx.prefix") String prefix,
                           @Named("influx.database") String database,
                           @Named("influx.user") String user,
                           @Named("influx.password") String password,
                           @Named("influx.rateSeconds") int rateSeconds
        ) throws Exception {
        if (!enable) {
            logger.info("influx metrics not enabled");
            return;
        }
        logger.info("starting");
        if (registerJvm) {
            registerJvmMetrics(registry);
        }

        InfluxConfig config = InfluxConfig.builder()
                .registry(registry)
                .prefix(prefix)
                .influxDB(InfluxDBFactory.connect(hosts, user, password))
                .database(database)
                .rateUnit(TimeUnit.SECONDS)
                .durationUnit(TimeUnit.MILLISECONDS)
                .filter(new HubMetricsFilter())
                .build();
        InfluxReporter reporter = new InfluxReporter(config);
        reporter.start(rateSeconds, TimeUnit.SECONDS);
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

package com.flightstats.hub.metrics;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.graphite.Graphite;
import com.codahale.metrics.graphite.GraphiteReporter;
import com.codahale.metrics.jvm.GarbageCollectorMetricSet;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;
import com.codahale.metrics.jvm.ThreadStatesGaugeSet;
import com.conducivetech.services.common.util.Haltable;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.flightstats.jerseyguice.metrics.KeyPrefixedMetricSet.prefix;

public class GraphiteReporting implements Haltable {
    private static final Logger logger = LoggerFactory.getLogger(GraphiteReporting.class);

    private final List<GraphiteReporter> reporters = new LinkedList<>();

    @Inject
    public GraphiteReporting(MetricRegistry registry,
                             @Named("graphite.enable") boolean enable,
                             @Named("graphite.registerJvmMetrics") boolean registerJvm,
                             @Named("graphite.host") String host,
                             @Named("graphite.port") int port,
                             @Named("graphite.prefix") String graphitePrefix,
                             @Named("graphite.rateSeconds") int rateSeconds
                             ) {
        if (!enable) {
            logger.info("graphite metrics not enabled");
            return;
        }
        try {
            if (registerJvm) {
                registerJvmMetrics(registry);
            }
            final String prefix = graphitePrefix + "." + InetAddress.getLocalHost().getHostName().split("\\.")[0];
                logger.info("Enabling Graphite metrics for " + host + ":" + port + " - reporting interval " + rateSeconds + " seconds");
                final Graphite graphite = new Graphite(new InetSocketAddress(host, port));
                final GraphiteReporter reporter = GraphiteReporter.forRegistry(registry)
                        .prefixedWith(prefix)
                        .convertRatesTo(TimeUnit.SECONDS)
                        .convertDurationsTo(TimeUnit.MILLISECONDS)
                        .filter(new HubMetricsFilter())
                        .build(graphite);

                reporter.start(rateSeconds, TimeUnit.SECONDS);
                reporters.add(reporter);
        } catch (Exception filledWithHate) {
            throw new RuntimeException("Failure while enabling MetricsReporting", filledWithHate);
        }
    }

    private void registerJvmMetrics(MetricRegistry registry) {
        registry.registerAll(prefix("jvm.gc", new GarbageCollectorMetricSet()));
        registry.registerAll(prefix("jvm.memory", new MemoryUsageGaugeSet()));
        registry.registerAll(prefix("jvm.threads", new ThreadStatesGaugeSet()));
    }

    @Override
    public void halt() {
        for (GraphiteReporter reporter : reporters)
            reporter.stop();
    }
}

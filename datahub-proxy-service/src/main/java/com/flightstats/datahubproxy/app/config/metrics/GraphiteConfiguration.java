package com.flightstats.datahubproxy.app.config.metrics;

import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.graphite.Graphite;
import com.codahale.metrics.graphite.GraphiteReporter;
import com.codahale.metrics.jvm.GarbageCollectorMetricSet;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;
import com.codahale.metrics.jvm.ThreadStatesGaugeSet;
import com.flightstats.datahub.app.config.metrics.KeyPrefixedMetricSet;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

public class GraphiteConfiguration {

    @Inject
    public GraphiteConfiguration(
            MetricRegistry registry, @Named("graphite.host") String graphiteHost, @Named("graphite.port") int graphitePort, @Named("graphite.enabled") Boolean graphiteEnabled) {
        if (graphiteEnabled != null && !graphiteEnabled) {
            return;
        }
        createJvmMetrics(registry);
        createReporter(registry, graphiteHost, graphitePort);
    }

    private void createJvmMetrics(MetricRegistry registry) {
        registry.registerAll(KeyPrefixedMetricSet.prefix("jvm.gc", new GarbageCollectorMetricSet()));
        registry.registerAll(KeyPrefixedMetricSet.prefix("jvm.memory", new MemoryUsageGaugeSet()));
        registry.registerAll(KeyPrefixedMetricSet.prefix("jvm.threads", new ThreadStatesGaugeSet()));
    }

    private void createReporter(MetricRegistry registry, String graphiteHost, int graphitePort) {
        final Graphite graphite = new Graphite(new InetSocketAddress(graphiteHost, graphitePort));
        final GraphiteReporter reporter = GraphiteReporter.forRegistry(registry)
                .prefixedWith(buildPrefix())
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .filter(MetricFilter.ALL)
                .build(graphite);
        reporter.start(5, TimeUnit.SECONDS);
    }

    private String buildPrefix() {
        try {
            return "datahubproxy." + InetAddress.getLocalHost().getHostName().replaceFirst("\\..*", "");
        } catch (UnknownHostException e) {
            throw new RuntimeException("Error determining local hostname");
        }
    }

}

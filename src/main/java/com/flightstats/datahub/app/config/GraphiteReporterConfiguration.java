package com.flightstats.datahub.app.config;

import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.graphite.Graphite;
import com.codahale.metrics.graphite.GraphiteReporter;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

public class GraphiteReporterConfiguration {

	@Inject
	public GraphiteReporterConfiguration(MetricRegistry registry, @Named("graphite.host") String graphiteHost,
										 @Named("graphite.port") int graphitePort) {
		final Graphite graphite = new Graphite(new InetSocketAddress(graphiteHost, graphitePort));
		final GraphiteReporter reporter = GraphiteReporter.forRegistry(registry)
														  .prefixedWith("datahub")
														  .convertRatesTo(TimeUnit.SECONDS)
														  .convertDurationsTo(TimeUnit.MILLISECONDS)
														  .filter(MetricFilter.ALL)
														  .build(graphite);
		reporter.start(15, TimeUnit.SECONDS);
	}
}

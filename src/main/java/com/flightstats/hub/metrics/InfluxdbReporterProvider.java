package com.flightstats.hub.metrics;

import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;
import com.google.inject.Inject;
import com.google.inject.Provider;
import metrics_influxdb.HttpInfluxdbProtocol;
import metrics_influxdb.InfluxdbProtocol;
import metrics_influxdb.InfluxdbReporter;
import metrics_influxdb.UdpInfluxdbProtocol;
import java.util.concurrent.TimeUnit;

public class InfluxdbReporterProvider implements Provider<ScheduledReporter> {
    private final MetricsConfig metricsConfig;
    private final MetricRegistry metricRegistry;

    @Inject
    public InfluxdbReporterProvider(MetricsConfig metricsConfig, MetricRegistry metricRegistry) {
        this.metricsConfig = metricsConfig;
        this.metricRegistry = metricRegistry;
    }

    private UdpInfluxdbProtocol udpProtocol() {
        return new UdpInfluxdbProtocol(metricsConfig.getInfluxdbHost(), metricsConfig.getInfluxdbPort());
    }

    private HttpInfluxdbProtocol httpProtocol() {
        return new HttpInfluxdbProtocol(
                metricsConfig.getInfluxdbHost(),
                metricsConfig.getInfluxdbPort(),
                metricsConfig.getInfluxdbUser(),
                metricsConfig.getInfluxdbPass(),
                metricsConfig.getInfluxdbDatabaseName());
    }

    @Override
    public ScheduledReporter get() {
        String protocolId = metricsConfig.getInfluxdbProtocol();
        InfluxdbProtocol protocol = protocolId.contains("http") ? httpProtocol() : udpProtocol();
        return InfluxdbReporter.forRegistry(metricRegistry)
            .protocol(protocol)
            .convertRatesTo(TimeUnit.SECONDS)
            .convertDurationsTo(TimeUnit.MILLISECONDS)
            .filter(MetricFilter.ALL)
            .skipIdleMetrics(false)
            .tag("role", metricsConfig.getRole())
            .tag("team", metricsConfig.getTeam())
            .tag("env", metricsConfig.getEnv())
            .tag("cluster", metricsConfig.getClusterTag())
            .tag("host", metricsConfig.getHostTag())
            .tag("version", metricsConfig.getAppVersion())
            .build();
    }
}

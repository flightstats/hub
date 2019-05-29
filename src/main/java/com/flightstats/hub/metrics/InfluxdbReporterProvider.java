package com.flightstats.hub.metrics;

import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;
import com.flightstats.hub.app.HubVersion;
import com.flightstats.hub.config.properties.LocalHostProperties;
import com.flightstats.hub.config.properties.MetricsProperties;
import com.flightstats.hub.config.properties.TickMetricsProperties;
import com.google.inject.Inject;
import com.google.inject.Provider;
import metrics_influxdb.HttpInfluxdbProtocol;
import metrics_influxdb.InfluxdbProtocol;
import metrics_influxdb.InfluxdbReporter;
import metrics_influxdb.UdpInfluxdbProtocol;

import java.util.concurrent.TimeUnit;

public class InfluxdbReporterProvider implements Provider<ScheduledReporter> {
    private final TickMetricsProperties tickMetricsProperties;
    private final MetricsProperties metricsProperties;
    private final MetricRegistry metricRegistry;
    private final String hostName;
    private final HubVersion hubVersion;

    @Inject
    public InfluxdbReporterProvider(TickMetricsProperties tickMetricsProperties,
                                    MetricsProperties metricsProperties,
                                    MetricRegistry metricRegistry,
                                    LocalHostProperties localHostProperties,
                                    HubVersion hubVersion) {
        this.tickMetricsProperties = tickMetricsProperties;
        this.metricsProperties = metricsProperties;
        this.metricRegistry = metricRegistry;
        this.hostName = localHostProperties.getName();
        this.hubVersion = hubVersion;
    }

    private UdpInfluxdbProtocol udpProtocol() {
        return new UdpInfluxdbProtocol(tickMetricsProperties.getInfluxDbHost(), tickMetricsProperties.getInfluxDbPort());
    }

    private HttpInfluxdbProtocol httpProtocol() {
        return new HttpInfluxdbProtocol(
                tickMetricsProperties.getInfluxDbHost(),
                tickMetricsProperties.getInfluxDbPort(),
                tickMetricsProperties.getInfluxDbUser(),
                tickMetricsProperties.getInfluxDbPassword(),
                tickMetricsProperties.getInfluxDbName());
    }

    @Override
    public ScheduledReporter get() {
        String protocolId = tickMetricsProperties.getInfluxDbProtocol();
        InfluxdbProtocol protocol = protocolId.contains("http") ? httpProtocol() : udpProtocol();
        return InfluxdbReporter.forRegistry(metricRegistry)
                .protocol(protocol)
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .filter(MetricFilter.ALL)
                .skipIdleMetrics(false)
                .tag("role", metricsProperties.getRoleTag())
                .tag("team", metricsProperties.getTeamTag())
                .tag("env", metricsProperties.getEnv())
                .tag("cluster", metricsProperties.getClusterTag())
                .tag("host", hostName)
                .tag("version", hubVersion.getVersion())
                .build();
    }
}

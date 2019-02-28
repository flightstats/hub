package com.flightstats.hub.metrics;

import com.flightstats.hub.app.HubHost;
import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.app.HubServices;
import com.flightstats.hub.app.HubVersion;
import com.google.inject.Inject;
import com.google.inject.Provider;

public class MetricsConfigProvider implements Provider<MetricsConfig> {
    private HubVersion hubVersion;

    @Inject
    public MetricsConfigProvider(HubVersion hubVersion) {
        this.hubVersion = hubVersion;
    }

    @Override
    public  MetricsConfig get() {
        return MetricsConfig.builder()
                .appVersion(hubVersion.getVersion())
                .clusterTag(
                        HubProperties.getProperty("cluster.location", "local") +
                                "-" +
                                HubProperties.getProperty("app.environment", "dev")
                )
                .env(HubProperties.getProperty("app.environment", "dev"))
                .enabled(HubProperties.getProperty("metrics.enable", "false").equals("true"))
                .hostTag(HubHost.getLocalName())
                .influxdbDatabaseName(HubProperties.getProperty("metrics.influxdb.database.name", "hub_tick"))
                .influxdbHost(HubProperties.getProperty("metrics.influxdb.host", "localhost"))
                .influxdbPass(HubProperties.getProperty("metrics.influxdb.database.password", ""))
                .influxdbPort(HubProperties.getProperty("metrics.influxdb.port", 8086))
                .influxdbProtocol(HubProperties.getProperty("metrics.influxdb.protocol", "http"))
                .influxdbUser(HubProperties.getProperty("metrics.influxdb.database.user", ""))
                .reportingIntervalSeconds(HubProperties.getProperty("metrics.seconds", 15))
                .role(HubProperties.getProperty("metrics.tags.role", "hub"))
                .statsdPort(HubProperties.getProperty("metrics.statsd.port", 8124))
                .dogstatsdPort(HubProperties.getProperty("metrics.dogstatsd.port", 8125))
                .datadogApiUrl(HubProperties.getProperty("metrics.datadog.url", "https://app.datadoghq.com/api/v1"))
                .team(HubProperties.getProperty("metrics.tags.team", "development"))
                .build();
    }
}

package com.flightstats.hub.metrics;

import com.flightstats.hub.app.HubHost;
import com.flightstats.hub.app.HubVersion;
import com.flightstats.hub.config.AppProperties;
import com.flightstats.hub.config.DatadogMetricsProperties;
import com.flightstats.hub.config.TickMetricsProperties;
import com.google.inject.Provider;

import javax.inject.Inject;

public class MetricsConfigProvider implements Provider<MetricsConfig> {

    private HubVersion hubVersion;
    private final AppProperties appProperties;
    private final TickMetricsProperties tickMetricsProperty;
    private final DatadogMetricsProperties datadogMetricsProperties;

    @Inject
    public MetricsConfigProvider(HubVersion hubVersion,
                                 AppProperties appProperties,
                                 TickMetricsProperties tickMetricsProperty,
                                 DatadogMetricsProperties datadogMetricsProperties) {
        this.hubVersion = hubVersion;
        this.appProperties = appProperties;
        this.tickMetricsProperty = tickMetricsProperty;
        this.datadogMetricsProperties = datadogMetricsProperties;
    }

    @Override
    public MetricsConfig get() {
        return MetricsConfig.builder()
                .appVersion(hubVersion.getVersion())
                .clusterTag(appProperties.getClusterLocation() + "-" + appProperties.getEnv())
                .env(appProperties.getEnv())
                .enabled(tickMetricsProperty.isMetricsEnable())
                .team(tickMetricsProperty.getMetricsTagsTeam())
                .role(tickMetricsProperty.getMetricsTagsRole())
                .reportingIntervalSeconds(tickMetricsProperty.getMetricsSeconds())
                .hostTag(HubHost.getLocalName())
                .influxdbDatabaseName(tickMetricsProperty.getInfluxDbName())
                .influxdbHost(tickMetricsProperty.getInfluxDbHost())
                .influxdbPass(tickMetricsProperty.getInfluxDbPassword())
                .influxdbPort(tickMetricsProperty.getInfluxDbPort())
                .influxdbProtocol(tickMetricsProperty.getInfluxDbProtocol())
                .influxdbUser(tickMetricsProperty.getInfluxDbUser())
                .statsdPort(tickMetricsProperty.getStatsdPort())
                .dogstatsdPort(datadogMetricsProperties.getStatsdPort())
                .datadogApiUrl(datadogMetricsProperties.getUrl())
                .dataDogAppKey(datadogMetricsProperties.getAppKey())
                .dataDogAPIKey(datadogMetricsProperties.getApiKey())
                .build();
    }
}
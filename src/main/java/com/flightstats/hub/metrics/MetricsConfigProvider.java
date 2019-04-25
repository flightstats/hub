package com.flightstats.hub.metrics;

import com.flightstats.hub.app.HubHost;
import com.flightstats.hub.app.HubVersion;
import com.flightstats.hub.config.AppProperty;
import com.flightstats.hub.config.DatadogMetricsProperty;
import com.flightstats.hub.config.TickMetricsProperty;
import com.google.inject.Provider;

import javax.inject.Inject;

public class MetricsConfigProvider implements Provider<MetricsConfig> {

    private HubVersion hubVersion;
    private AppProperty appProperty;
    private TickMetricsProperty tickMetricsProperty;
    private DatadogMetricsProperty datadogMetricsProperty;

    @Inject
    public MetricsConfigProvider(HubVersion hubVersion,
                                 AppProperty appProperty,
                                 TickMetricsProperty tickMetricsProperty,
                                 DatadogMetricsProperty datadogMetricsProperty) {
        this.hubVersion = hubVersion;
        this.appProperty = appProperty;
        this.tickMetricsProperty = tickMetricsProperty;
        this.datadogMetricsProperty = datadogMetricsProperty;
    }

    @Override
    public MetricsConfig get() {
        return MetricsConfig.builder()
                .appVersion(hubVersion.getVersion())
                .clusterTag(appProperty.getClusterLocation() + "-" + appProperty.getEnv())
                .env(appProperty.getEnv())
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
                .dogstatsdPort(datadogMetricsProperty.getStatsdPort())
                .datadogApiUrl(datadogMetricsProperty.getUrl())
                .dataDogAppKey(datadogMetricsProperty.getAppKey())
                .dataDogAPIKey(datadogMetricsProperty.getApiKey())
                .build();
    }
}
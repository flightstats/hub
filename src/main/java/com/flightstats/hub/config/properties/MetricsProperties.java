package com.flightstats.hub.config.properties;

import com.flightstats.hub.app.HubHost;

import javax.inject.Inject;

public class MetricsProperties {

    private final PropertiesLoader propertiesLoader;
    private final AppProperties appProperties;

    @Inject
    public MetricsProperties(PropertiesLoader propertiesLoader, AppProperties appProperties) {
        this.propertiesLoader = propertiesLoader;
        this.appProperties = appProperties;
    }

    public boolean isEnabled() {
        return propertiesLoader.getProperty("metrics.enable", false);
    }

    public String getEnv(){
        return appProperties.getEnv();
    }

    public int getReportingIntervalInSeconds() {
        return propertiesLoader.getProperty("metrics.seconds", 15);
    }

    public String getRoleTag() {
        return propertiesLoader.getProperty("metrics.tags.role", "hub");
    }

    public String getTeamTag() {
        return propertiesLoader.getProperty("metrics.tags.team", "development");
    }

    public String getHostTag() {
        return HubHost.getLocalName();
    }

    public String getClusterTag(){
        return appProperties.getClusterLocation() + "-" + appProperties.getEnv();
    }
}

package com.flightstats.hub.config.properties;

import javax.inject.Inject;

public class TickMetricsProperties {

    private final PropertiesLoader propertiesLoader;

    @Inject
    public TickMetricsProperties(PropertiesLoader propertiesLoader) {
        this.propertiesLoader = propertiesLoader;
    }

    public String getInfluxDbHost() {
        return propertiesLoader.getProperty("metrics.influxdb.host", "localhost");
    }

    public int getInfluxDbPort() {
        return propertiesLoader.getProperty("metrics.influxdb.port", 8086);
    }

    public int getStatsdPort() {
        return propertiesLoader.getProperty("metrics.statsd.port", 8124);
    }

    public String getInfluxDbProtocol() {
        return propertiesLoader.getProperty("metrics.influxdb.protocol", "http");
    }

    public String getInfluxDbName() {
        return propertiesLoader.getProperty("metrics.influxdb.database.name", "hub_tick");
    }

    public String getInfluxDbPassword() {
        return propertiesLoader.getProperty("metrics.influxdb.database.password", "");
    }

    public String getInfluxDbUser() {
        return propertiesLoader.getProperty("metrics.influxdb.database.user", "");
    }

    //These properties are used for both datadog and Tick
    public boolean isMetricsEnabled() {
        return propertiesLoader.getProperty("metrics.enable", false);
    }

    public int getMetricsSeconds() {
        return propertiesLoader.getProperty("metrics.seconds", 15);
    }

    public String getMetricsTagsRole() {
        return propertiesLoader.getProperty("metrics.tags.role", "hub");
    }

    public String getMetricsTagsTeam() {
        return propertiesLoader.getProperty("metrics.tags.team", "development");
    }
}
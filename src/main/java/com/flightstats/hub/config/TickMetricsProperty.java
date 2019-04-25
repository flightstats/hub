package com.flightstats.hub.config;

import javax.inject.Inject;

public class TickMetricsProperty {

    private PropertyLoader propertyLoader;

    @Inject
    public TickMetricsProperty(PropertyLoader propertyLoader) {
        this.propertyLoader = propertyLoader;
    }

    public String getInfluxDbHost() {
        return this.propertyLoader.getProperty("metrics.influxdb.host", "localhost");
    }

    public int getInfluxDbPort() {
        return this.propertyLoader.getProperty("metrics.influxdb.port", 8086);
    }

    public int getStatsdPort() {
        return this.propertyLoader.getProperty("metrics.statsd.port", 8124);
    }

    public String getInfluxDbProtocol() {
        return this.propertyLoader.getProperty("metrics.influxdb.protocol", "http");
    }

    public String getInfluxDbName() {
        return this.propertyLoader.getProperty("metrics.influxdb.database.name", "hub_tick");
    }

    public String getInfluxDbPassword() {
        return this.propertyLoader.getProperty("metrics.influxdb.database.password", "");
    }

    public String getInfluxDbUser() {
        return this.propertyLoader.getProperty("metrics.influxdb.database.user", "");
    }

    //These properties are used for both datadog and Tick
    public boolean isMetricsEnable() {
        return this.propertyLoader.getProperty("metrics.enable", false);
    }

    public int getMetricsSeconds() {
        return this.propertyLoader.getProperty("metrics.seconds", 15);
    }

    public String getMetricsTagsRole() {
        return this.propertyLoader.getProperty("metrics.tags.role", "hub");
    }

    public String getMetricsTagsTeam() {
        return this.propertyLoader.getProperty("metrics.tags.team", "development");
    }
}
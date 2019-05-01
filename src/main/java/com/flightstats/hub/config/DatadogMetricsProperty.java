package com.flightstats.hub.config;

import javax.inject.Inject;

public class DatadogMetricsProperty {

    private PropertyLoader propertyLoader;

    @Inject
    public DatadogMetricsProperty(PropertyLoader propertyLoader) {
        this.propertyLoader = propertyLoader;
    }

    public String getUrl() {
        return this.propertyLoader.getProperty("metrics.datadog.url", "https://app.datadoghq.com/api/v1");
    }

    public String getAppKey() {
        return this.propertyLoader.getProperty("metrics.data_dog.app_key", "");
    }

    public String getApiKey() {
        return this.propertyLoader.getProperty("metrics.data_dog.api_key", "");
    }

    public int getStatsdPort() {
        return this.propertyLoader.getProperty("metrics.dogstatsd.port", 8125);
    }
}
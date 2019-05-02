package com.flightstats.hub.config;

import javax.inject.Inject;

public class DatadogMetricsProperties {

    private final PropertiesLoader propertiesLoader;

    @Inject
    public DatadogMetricsProperties(PropertiesLoader propertiesLoader) {
        this.propertiesLoader = propertiesLoader;
    }

    public String getUrl() {
        return this.propertiesLoader.getProperty("metrics.datadog.url", "https://app.datadoghq.com/api/v1");
    }

    public String getAppKey() {
        return this.propertiesLoader.getProperty("metrics.data_dog.app_key", "");
    }

    public String getApiKey() {
        return this.propertiesLoader.getProperty("metrics.data_dog.api_key", "");
    }

    public int getStatsdPort() {
        return this.propertiesLoader.getProperty("metrics.dogstatsd.port", 8125);
    }
}
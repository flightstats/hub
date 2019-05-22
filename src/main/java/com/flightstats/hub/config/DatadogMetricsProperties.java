package com.flightstats.hub.config;

import javax.inject.Inject;

public class DatadogMetricsProperties {

    private final PropertiesLoader propertiesLoader;

    @Inject
    public DatadogMetricsProperties(PropertiesLoader propertiesLoader) {
        this.propertiesLoader = propertiesLoader;
    }

    public String getUrl() {
        return propertiesLoader.getProperty("metrics.datadog.url", "https://app.datadoghq.com/api/v1");
    }

    public String getAppKey() {
        return propertiesLoader.getProperty("metrics.data_dog.app_key", "");
    }

    public String getApiKey() {
        return propertiesLoader.getProperty("metrics.data_dog.api_key", "");
    }

    public int getStatsdPort() {
        return propertiesLoader.getProperty("metrics.dogstatsd.port", 8125);
    }
}
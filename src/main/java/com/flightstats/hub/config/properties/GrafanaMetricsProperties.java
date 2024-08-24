package com.flightstats.hub.config.properties;

import javax.inject.Inject;

public class GrafanaMetricsProperties {
    private final PropertiesLoader propertiesLoader;

    @Inject
    public GrafanaMetricsProperties(PropertiesLoader propertiesLoader) {
        this.propertiesLoader = propertiesLoader;
    }

    public String getApiUrl() {
        return propertiesLoader.getProperty("metrics.grafana.url", "https://prometheus-prod-24-prod-eu-west-2.grafana.net/api/prom/push");
    }

    public String getAppKey() {
        return propertiesLoader.getProperty("metrics.grafana.app_key", "");
    }

    public String getApiKey() {
        return propertiesLoader.getProperty("metrics.grafana.api_key", "");
    }

   /* public int getStatsdPort() {
        return propertiesLoader.getProperty("metrics.grafanastatsd.port", 9125);
    }*/

    public boolean isPrimary() {
        return propertiesLoader.getProperty("metrics.grafana.primary", true);
    }

    public String getRequestMetricsToIgnore() {
        return propertiesLoader.getProperty("metrics.grafana.request.ignore", "");
    }
}

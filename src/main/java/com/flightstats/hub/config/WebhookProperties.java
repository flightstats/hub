package com.flightstats.hub.config;

import javax.inject.Inject;

public class WebhookProperties {

    private final PropertiesLoader propertiesLoader;

    @Inject
    public WebhookProperties(PropertiesLoader propertiesLoader) {
        this.propertiesLoader = propertiesLoader;
    }

    public boolean isWebhookLeadershipEnabled() {
        return propertiesLoader.getProperty("webhook.leadership.enabled", true);
    }

    public int getCallbackTimeoutMinimum() {
        return propertiesLoader.getProperty("webhook.callbackTimeoutSeconds.min", 1);
    }

    public int getCallbackTimeoutMaximum() {
        return propertiesLoader.getProperty("webhook.callbackTimeoutSeconds.max", 1800);
    }

    public int getCallbackTimeoutDefaultInSec() {
        return propertiesLoader.getProperty("webhook.callbackTimeoutSeconds.default", 120);
    }

    public int getConnectTimeoutSeconds() {
        return propertiesLoader.getProperty("webhook.connectTimeoutSeconds", 60);
    }

    public int getReadTimeoutSeconds() {
        return propertiesLoader.getProperty("webhook.readTimeoutSeconds", 60);
    }

    public int getShutdownThreadCount() {
        return propertiesLoader.getProperty("webhook.shutdown.threads", 100);
    }

}
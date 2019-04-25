package com.flightstats.hub.config;

import javax.inject.Inject;

public class WebhookProperty {

    private PropertyLoader propertyLoader;

    @Inject
    public WebhookProperty(PropertyLoader propertyLoader) {
        this.propertyLoader = propertyLoader;
    }

    public int getCallbackTimeoutMin() {
        return this.propertyLoader.getProperty("webhook.callbackTimeoutSeconds.min", 1);
    }

    public int getCallbackTimeoutMax() {
        return this.propertyLoader.getProperty("webhook.callbackTimeoutSeconds.max", 1800);
    }

    public int getCallbackTimeoutDefaultInSec() {
        return this.propertyLoader.getProperty("webhook.callbackTimeoutSeconds.default", 120);
    }

    public int getConnectTimeoutSeconds() {
        return this.propertyLoader.getProperty("webhook.connectTimeoutSeconds", 60);
    }

    public int getReadTimeoutSeconds() {
        return this.propertyLoader.getProperty("webhook.readTimeoutSeconds", 60);
    }

    public int getShutdownThreadCount() {
        return this.propertyLoader.getProperty("webhook.shutdown.threads", 100);
    }

}
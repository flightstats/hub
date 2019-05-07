package com.flightstats.hub.config;

import javax.inject.Inject;

public class ContentProperties {

    private final PropertiesLoader propertiesLoader;

    @Inject
    public ContentProperties(PropertiesLoader propertiesLoader) {
        this.propertiesLoader = propertiesLoader;
    }

    public int getMaxPayloadSizeInMB() {
        return this.propertiesLoader.getProperty("app.maxPayloadSizeMB", 40);
    }

    public long getLargePayload() {
        return this.propertiesLoader.getProperty("app.large.payload.MB", 40) * 1024 * 1024;
    }

    public int getDirectionCountLimit() {
        return this.propertiesLoader.getProperty("app.directionCountLimit", 10000);
    }

    public boolean isChannelProtectionEnabled() {
        return this.propertiesLoader.getProperty("hub.protect.channels", true);
    }

    public int getStableSeconds() {
        return this.propertiesLoader.getProperty("app.stable_seconds", 5);
    }

    public int getQueryMergeMaxWaitInMins() {
        return this.propertiesLoader.getProperty("query.merge.max.wait.minutes", 2);
    }

}

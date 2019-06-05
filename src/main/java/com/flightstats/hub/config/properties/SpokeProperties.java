package com.flightstats.hub.config.properties;

import com.flightstats.hub.spoke.SpokeStore;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;

public class SpokeProperties {

    private final PropertiesLoader propertiesLoader;

    @Inject
    public SpokeProperties(PropertiesLoader propertiesLoader) {
        this.propertiesLoader = propertiesLoader;
    }

    public boolean isReplicationEnabled() {
        return propertiesLoader.getProperty("replication.enabled", true);
    }

    public int getTtlMinutes(SpokeStore spokeStore) {
        String property = "spoke." + spokeStore + ".ttlMinutes";
        String fallbackProperty = "spoke.ttlMinutes";
        return propertiesLoader.getProperty(property, propertiesLoader.getProperty(fallbackProperty, 60));
    }

    public String getPath(SpokeStore spokeStore) {
        String property = "spoke." + spokeStore + ".path";
        String fallbackProperty = "spoke.path";
        String defaultPath = "/spoke/" + spokeStore;
        return propertiesLoader.getProperty(property, propertiesLoader.getProperty(fallbackProperty, defaultPath));
    }

    public String getStoragePath() {
        String storagePath = propertiesLoader.getProperty("storage.path", "/file");
        return StringUtils.appendIfMissing(storagePath, "/");
    }

    public String getContentPath() {
        return getStoragePath() + "content/";
    }

    public boolean isTtlEnforced() {
        return propertiesLoader.getProperty("spoke.enforceTTL", true);
    }

    public int getWriteFactor() {
        return propertiesLoader.getProperty("spoke.write.factor", 3);
    }

}

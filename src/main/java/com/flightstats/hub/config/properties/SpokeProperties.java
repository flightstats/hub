package com.flightstats.hub.config.properties;

import com.flightstats.hub.spoke.SpokeStore;

import javax.inject.Inject;

public class SpokeProperties {

    private static int DEFAULT_SPOKE_TTL = 60;

    private final PropertiesLoader propertiesLoader;

    @Inject
    public SpokeProperties(PropertiesLoader propertiesLoader) {
        this.propertiesLoader = propertiesLoader;
    }

    public boolean isReplicationEnabled() { return propertiesLoader.getProperty("replication.enabled", true); }

    public int getTtlMinutes(SpokeStore spokeStore) {
        String property = "spoke." + spokeStore + ".ttlMinutes";
        String fallbackProperty = "spoke.ttlMinutes";
        return propertiesLoader.getProperty(property, propertiesLoader.getProperty(fallbackProperty, DEFAULT_SPOKE_TTL));
    }

    public String getPath(SpokeStore spokeStore) {
        String property = "spoke." + spokeStore + ".path";
        String fallbackProperty = "spoke.path";
        String defaultPath = "/spoke/" + spokeStore;
        return propertiesLoader.getProperty(property, propertiesLoader.getProperty(fallbackProperty, defaultPath));
    }

    public String getStoragePath() {
        return propertiesLoader.getProperty("storage.path", "/file");
    }

    public boolean isTtlEnforced() {
        return propertiesLoader.getProperty("spoke.enforceTTL", true);
    }

    public int getWriteFactor() {
        return propertiesLoader.getProperty("spoke.write.factor", 3);
    }
}

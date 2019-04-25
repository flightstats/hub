package com.flightstats.hub.config;

import com.flightstats.hub.spoke.SpokeStore;

import javax.inject.Inject;

public class SpokeProperty {

    private static int DEFAULT_SPOKE_TTL = 60;

    private PropertyLoader propertyLoader;

    @Inject
    public SpokeProperty(PropertyLoader propertyLoader) {
        this.propertyLoader = propertyLoader;
    }

    public int getTtlMinutes(SpokeStore spokeStore) {
        String property = "spoke." + spokeStore + ".ttlMinutes";
        String fallbackProperty = "spoke.ttlMinutes";
        return this.propertyLoader.getProperty(property, this.propertyLoader.getProperty(fallbackProperty, DEFAULT_SPOKE_TTL));
    }

    public String getPath(SpokeStore spokeStore) {
        String property = "spoke." + spokeStore + ".path";
        String fallbackProperty = "spoke.path";
        String defaultPath = "/spoke/" + spokeStore;
        return this.propertyLoader.getProperty(property, this.propertyLoader.getProperty(fallbackProperty, defaultPath));
    }

    public String getStoragePath() {
        return this.propertyLoader.getProperty("storage.path", "/file");
    }

    public boolean isEnforceTTL() {
        return this.propertyLoader.getProperty("spoke.enforceTTL", true);
    }

    public int getWriteFactor() {
        return this.propertyLoader.getProperty("spoke.write.factor", 3);
    }
}

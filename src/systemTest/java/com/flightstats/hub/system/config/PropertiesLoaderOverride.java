package com.flightstats.hub.system.config;

import lombok.Value;

import java.util.Properties;

@Value
public class PropertiesLoaderOverride {
    private final Properties properties;

    public PropertiesLoaderOverride(Properties properties) {
        this.properties = properties;
    }

    public PropertiesLoaderOverride withHubDockerImage(String image) {
        properties.setProperty(PropertiesName.HUB_DOCKER_IMAGE, image);
        return this;
    }

    public String getDefaultHubDockerImage() {
        return properties.getProperty(PropertiesName.HUB_DOCKER_IMAGE);
    }

}

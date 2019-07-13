package com.flightstats.hub.system.config;

import lombok.Value;

import java.util.Properties;

@Value
public class CustomPropertiesLoader {
    private final Properties properties;

    public CustomPropertiesLoader(Properties properties) {
        this.properties = properties;
    }

    public CustomPropertiesLoader withHubDockerImage(String image) {
        properties.setProperty(PropertiesName.HUB_DOCKER_IMAGE, image);
        return this;
    }

}

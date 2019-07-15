package com.flightstats.hub.system.config;

import com.flightstats.hub.system.resiliency.PlaceholderTest;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import java.util.Properties;

@Value
@Slf4j
public class PropertiesLoaderOverride {
    private final Properties properties;

    public PropertiesLoaderOverride(Properties properties) {
        this.properties = properties;
    }


    private Properties placeholderTestRegister() {
        properties.setProperty(PropertiesName.HUB_DOCKER_IMAGE, PlaceholderTest.IMAGE);
        return properties;
    }

    public Properties get(Object test) {
        if (test.getClass().equals(PlaceholderTest.class)) {
            return placeholderTestRegister();
        }
        return properties;
    }

}

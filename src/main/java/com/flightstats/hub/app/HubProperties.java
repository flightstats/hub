package com.flightstats.hub.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public class HubProperties {
    private final static Logger logger = LoggerFactory.getLogger(HubProperties.class);
    private static Properties properties = new Properties();

    public static void setProperties(Properties properties) {
        logger.info("setting properties {}", properties);
        HubProperties.properties = properties;
    }

    public static int getProperty(String name, int defaultValue) {
        return Integer.parseInt(properties.getProperty(name, Integer.toString(defaultValue)));
    }

    public static String getProperty(String name, String defaultValue) {
        return properties.getProperty(name, defaultValue);
    }
}

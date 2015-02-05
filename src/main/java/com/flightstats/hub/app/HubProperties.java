package com.flightstats.hub.app;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

public class HubProperties {
    private final static Logger logger = LoggerFactory.getLogger(HubProperties.class);
    private static Properties properties = new Properties();

    public static void setProperties(Properties properties) {
        logger.info("setting properties {}", properties);
        HubProperties.properties = properties;
    }

    public static Properties getProperties() {
        return properties;
    }

    public static boolean getProperty(String name, boolean defaultValue) {
        return Boolean.parseBoolean(properties.getProperty(name, Boolean.toString(defaultValue)));
    }

    public static int getProperty(String name, int defaultValue) {
        return Integer.parseInt(properties.getProperty(name, Integer.toString(defaultValue)));
    }

    public static String getProperty(String name, String defaultValue) {
        return properties.getProperty(name, defaultValue);
    }

    public static Properties loadProperties(String fileName) throws MalformedURLException {
        Properties properties;
        if (fileName.equals("useDefault")) {
            properties = getLocalProperties("default");
        } else if (fileName.equals("useEncryptedDefault")) {
            properties = getLocalProperties("defaultEncrypted");
        } else {
            properties = loadProperties(new File(fileName).toURI().toURL(), true);
        }
        HubProperties.setProperties(properties);
        return properties;
    }

    private static Properties getLocalProperties(String fileNameRoot) {
        HubMain.warn("using " + fileNameRoot + " properties file");
        Properties defaultProperties = getProperties("/" + fileNameRoot + ".properties", true);
        Properties localProperties = getProperties("/" + fileNameRoot + "_local.properties", false);
        for (String localKey : localProperties.stringPropertyNames()) {
            String newVal = localProperties.getProperty(localKey);
            logger.info("overriding " + localKey + " using '" + newVal +
                    "' instead of '" + defaultProperties.getProperty(localKey, "") + "'");
            defaultProperties.setProperty(localKey, newVal);
        }
        return defaultProperties;
    }

    private static Properties getProperties(String name, boolean required) {
        return loadProperties(HubMain.class.getResource(name), required);
    }

    private static Properties loadProperties(URL url, boolean required) {
        Properties properties = new Properties();
        InputStream inputStream = null;
        try {
            inputStream = url.openStream();
            properties.load(inputStream);
        } catch (Exception e) {
            String message = "Unable to load required properties file from location: " + url;
            logger.error(message, e);
            if (required) {
                throw new RuntimeException(message, e);
            }
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
        return properties;
    }
}

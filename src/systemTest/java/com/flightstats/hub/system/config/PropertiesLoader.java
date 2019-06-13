package com.flightstats.hub.system.config;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.Properties;

import static com.flightstats.hub.util.StringUtils.randomAlphaNumeric;

@Slf4j
class PropertiesLoader {
    Properties loadProperties(String propertiesFileName) {
        Properties properties = new Properties();
        properties.putAll(loadFileProperties(propertiesFileName));
        properties.putAll(getSystemTestSystemProperties());

        properties.setProperty(PropertiesName.HELM_RELEASE_NAME, getHelmReleaseName(properties));
        properties.setProperty(PropertiesName.HELM_RELEASE_DELETE, isHelmReleaseDeletable(properties));
        properties.setProperty(PropertiesName.HELM_CLUSTERED_HUB, isHelmHubClustered(properties));

        return properties;
    }

    private Properties loadFileProperties(String propertiesFileName) {
        Properties properties = new Properties();
        try (InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(propertiesFileName)) {
            properties.load(inputStream);
        } catch (IOException e) {
            log.error("Property file {} not found", propertiesFileName, e);
        }
        return properties;
    }

    private Properties getSystemTestSystemProperties() {
        Properties properties = new Properties();
        System.getProperties().forEach((key, value) ->
                properties.put(
                        key.toString().replace("systemTest.", ""),
                        value));
        return properties;
    }

    private String getHelmReleaseName(Properties properties) {
        String randomReleaseName = "ddt-" + System.getProperty("user.name") + "-" + randomAlphaNumeric(4).toLowerCase();
        return properties.getProperty(PropertiesName.HELM_RELEASE_NAME, randomReleaseName);
    }

    private String isHelmReleaseDeletable(Properties properties) {
        return properties.getProperty(PropertiesName.HELM_RELEASE_DELETE, "true");
    }

    private String isHelmHubClustered(Properties properties) {
        return properties.getProperty(PropertiesName.HELM_CLUSTERED_HUB, "true");
    }
}

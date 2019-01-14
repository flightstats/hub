package com.flightstats.hub.metrics;

import com.flightstats.hub.app.HubVersion;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

import static com.flightstats.hub.app.HubHost.getLocalName;
import com.flightstats.hub.app.HubProvider;

public class MetricsConfig {
    private final Logger logger = LoggerFactory.getLogger(MetricsConfig.class);

    private Properties properties;

    @Getter
    private final String role = "hub";

    @Getter
    private final String team = "ddt";

    @Getter(lazy = true)
    private final String env = safeGetProperty("app.environment", "dev");

    @Getter(lazy = true)
    private final String clusterLocation = safeGetProperty("cluster.location", "local");

    @Getter(lazy = true)
    private final String influxdbHost = safeGetProperty("metrics.influxdb.host", "localhost");

    @Getter(lazy = true)
    private final int influxdbPort = safeGetProperty("metrics.influxdb.port", 8086);

    @Getter(lazy = true)
    private final String influxdbUser = safeGetProperty("metrics.influxdb.database.user", "");

    @Getter(lazy = true)
    private final String influxdbPass = safeGetProperty("metrics.influxdb.database.password", "");

    @Getter(lazy = true)
    private final String influxdbDatabaseName = safeGetProperty("metrics.influxdb.database.name", "hubmain");

    public MetricsConfig() {
        properties = new Properties();
    }

    public MetricsConfig(Properties customProperties) {
        properties = customProperties;
    }


    public boolean loadValues(String fileName) {
        try {
            URL fileUrl = new File(fileName).toURI().toURL();
            properties = loadProperties(fileUrl);
            return true;
        } catch (Exception e) {
            logger.error("error loading properties file from: {}", fileName, e);
            return false;
        }
    }

    private Properties loadProperties(URL url) {
        Properties properties = new Properties();
        try (InputStream inputStream = url.openStream()){
            properties.load(inputStream);
        } catch (Exception e) {
            String message = "Unable to load required properties file from location: " + url;
            logger.error(message, e);
            throw new RuntimeException(message, e);
        }
        return properties;
    }

    private int safeGetProperty(String key, int defaultValue) {
        String value = Integer.toString(defaultValue);
        return Integer.parseInt(safeGetProperty(key, value));
    }

    private String safeGetProperty(String key, String defaultValue) {
        try {
            if (properties != null) {
                return properties.getProperty(key, defaultValue);
            }
            return defaultValue;
        } catch (NullPointerException e) {
            logger.warn("expected config key: {} not set, using default value: {}", key, defaultValue);
            return defaultValue;
        }
    }


    public boolean enabled() {
        String key = "metrics.enable";
        try {
            return Boolean.parseBoolean(properties.getProperty(key, "false"));
        } catch (NullPointerException e) {
            logger.warn("expected config key: {} not set, using default value: {}", key, false);
            return false;
        }
    }

    public String getClusterTag() {
        return getClusterLocation() + "-" + getEnv();
    }

    public int getReportingIntervalSeconds() {
        return safeGetProperty("metrics.seconds", 15);
    }

    public String getAppVersion() {
        try {
            HubVersion hubVersion = HubProvider.getInstance(HubVersion.class);
            return hubVersion.getVersion();
            // catch null exception because HubProvider is not available in unit tests
        } catch (NullPointerException e) {
            logger.info("no app version available using 'local'", e);
            return "local";
        }
    }

    public String getHostTag() {
        try {
            return getLocalName();
        } catch (RuntimeException e) {
            logger.debug("unable to get HubHost.getLocalName() err: {}", e);
            return "unknown";
        }
    }

    public String getInfluxdbProtocol() {
        String protocol = safeGetProperty("metrics.influxdb.protocol", "http");
        // allow use of udp
        if (!"https,udp".contains(protocol.trim().toLowerCase())) {
            logger.warn("Invalid protocol for influxdb reporter - using http");
            protocol = "http";
        }
        return protocol;
    }
}

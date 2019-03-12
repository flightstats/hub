package com.flightstats.hub.app;

import com.flightstats.hub.spoke.SpokeStore;
import com.flightstats.hub.util.HubUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Properties;

@Deprecated
public class HubProperties {
    private final static Logger logger = LoggerFactory.getLogger(HubProperties.class);
    private static Properties properties = new Properties();

    public static boolean isReadOnly() {
        String readOnlyNodes = HubProperties.getProperty("hub.read.only", "");
        return Arrays.asList(readOnlyNodes.split(","))
                .contains(HubHost.getLocalName());
    }

    public static String getAppUrl() {
        return StringUtils.appendIfMissing(HubProperties.getProperty("app.url", ""), "/");
    }

    public static boolean isAppEncrypted() {
        return HubProperties.getProperty("app.encrypted", false);
    }

    public static int getSpokeTtlMinutes(SpokeStore spokeStore) {
        String property = "spoke." + spokeStore + ".ttlMinutes";
        String fallbackProperty = "spoke.ttlMinutes";
        int defaultTTL = 60;
        return getProperty(property, getProperty(fallbackProperty, defaultTTL));
    }

    public static String getAppEnv() {
        return (getProperty("app.name", "hub") + "_" + getProperty("app.environment", "unknown")).replace("-", "_");
    }

    public static boolean isProtected() {
        return HubProperties.getProperty("hub.protect.channels", true);
    }

    public static String getSpokePath(SpokeStore spokeStore) {
        String property = "spoke." + spokeStore + ".path";
        String fallbackProperty = "spoke.path";
        String defaultPath = "/spoke/" + spokeStore;
        return getProperty(property, getProperty(fallbackProperty, defaultPath));
    }

    public static long getLargePayload() {
        return HubProperties.getProperty("app.large.payload.MB", 40) * 1024 * 1024;
    }

    public static int getCallbackTimeoutMin() {
        return HubProperties.getProperty("webhook.callbackTimeoutSeconds.min", 1);
    }

    public static int getCallbackTimeoutMax() {
        return HubProperties.getProperty("webhook.callbackTimeoutSeconds.max", 1800);
    }

    public static int getCallbackTimeoutDefault() {
        return HubProperties.getProperty("webhook.callbackTimeoutSeconds.default", 120);
    }

    public static String getSigningRegion() {
        return getProperty("aws.signing_region", "us-east-1");
    }

    public static int getS3WriteQueueSize() {
        return getProperty("s3.writeQueueSize", 40000);
    }

    public static int getS3WriteQueueThreads() {
        return getProperty("s3.writeQueueThreads", 20);
    }

    static Properties getProperties() {
        return properties;
    }

    private static void setProperties(Properties properties) {
        logger.info("setting properties {}", properties);
        HubProperties.properties = properties;
    }

    public static boolean getProperty(String name, boolean defaultValue) {
        return Boolean.parseBoolean(properties.getProperty(name, Boolean.toString(defaultValue)));
    }

    public static int getProperty(String name, int defaultValue) {
        return Integer.parseInt(properties.getProperty(name, Integer.toString(defaultValue)));
    }

    public static double getProperty(String key, double defaultValue) {
        return Double.parseDouble(properties.getProperty(key, Double.toString(defaultValue)));
    }

    public static String getProperty(String name, String defaultValue) {
        return properties.getProperty(name, defaultValue);
    }

    public static Properties loadProperties(String fileName) throws MalformedURLException {
        Properties properties;
        if (fileName.equals("useDefault")) {
            properties = getLocalProperties("hub");
        } else if (fileName.equals("useEncryptedDefault")) {
            properties = getLocalProperties("hubEncrypted");
        } else {
            properties = loadProperties(new File(fileName).toURI().toURL(), true);
        }
        HubProperties.setProperties(properties);
        return properties;
    }

    private static Properties getLocalProperties(String fileNameRoot) {
        logger.warn("using " + fileNameRoot + " properties file");
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
        URL resource = HubMain.class.getResource(name);
        if (resource != null) {
            return loadProperties(resource, required);
        } else {
            logger.warn("unable to load files, using baked in defaults");
            Properties properties = new Properties();
            properties.put("hub.type", "aws");
            properties.put("app.name", "hub-v2");
            properties.put("dynamo.endpoint", "dynamodb.us-east-1.amazonaws.com");
            properties.put("app.environment", "local");
            properties.put("s3.environment", "local");
            properties.put("s3.endpoint", "s3-external-1.amazonaws.com");
            properties.put("s3.writeQueueSize", "2000");
            properties.put("dynamo.table_creation_wait_minutes", "10");
            properties.put("app.lib_path", "");
            properties.put("app.shutdown_delay_seconds", "2");
            properties.put("app.url", "http://localhost:9080/");
            properties.put("spoke.path", "/tmp/spoke/test");
            properties.put("spoke.ttlMinutes", "240");
            properties.put("http.bind_port", "9080");
            properties.put("hosted_graphite.enable", "false");
            properties.put("zookeeper.connection", "localhost:2181");
            properties.put("runSingleZookeeperInternally", "singleNode");
            properties.put("hub.protect.channels", "false");
            properties.put("metrics.enable", "false");
            properties.put("s3Verifier.run", "false");
            properties.put("aws.signing_region", "us-east-1");
            return properties;
        }
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
            HubUtils.closeQuietly(inputStream);
        }
        return properties;
    }

    public static void setProperty(String key, String value) {
        properties.put(key, value);
    }
}

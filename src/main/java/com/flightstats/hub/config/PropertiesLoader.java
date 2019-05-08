package com.flightstats.hub.config;

import com.flightstats.hub.app.HubMain;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

@Slf4j
public class PropertiesLoader {

    private static final String HUB_PROPERTY_FILE_NAME = "/hub.properties";
    private static final String HUB_ENCRYPTED_PROPERTY_FILE_NAME = "/hubEncrypted.properties";
    private static final PropertiesLoader propertiesLoader = new PropertiesLoader();
    private static Properties properties = new Properties();

    public static PropertiesLoader getInstance() {
        return propertiesLoader;
    }

    public Properties getProperties() {
        return properties;
    }

    public boolean getProperty(String name, boolean defaultValue) {
        return Boolean.parseBoolean(properties.getProperty(name, Boolean.toString(defaultValue)));
    }

    public int getProperty(String name, int defaultValue) {
        return Integer.parseInt(properties.getProperty(name, Integer.toString(defaultValue)));
    }

    public double getProperty(String key, double defaultValue) {
        return Double.parseDouble(properties.getProperty(key, Double.toString(defaultValue)));
    }

    public String getProperty(String name, String defaultValue) {
        return properties.getProperty(name, defaultValue);
    }

    public void setProperty(String key, String value) {
        properties.put(key, value);
    }

    public void load(String file) throws MalformedURLException {

        URL resource = new File(file).toURI().toURL();

        if (file.equals("useDefault")) {
            resource = HubMain.class.getResource(HUB_PROPERTY_FILE_NAME);
        } else if (file.equals("useEncryptedDefault")) {
            resource = HubMain.class.getResource(HUB_ENCRYPTED_PROPERTY_FILE_NAME);
        }

        if (resource == null) {
            setDefaultProperties();
        } else {
            try (InputStream inputStream = resource.openStream()) {
                properties.load(inputStream);
            } catch (Exception e) {
                log.error("Unable to load required properties file from location: {}", resource, e);
                throw new RuntimeException(e.getMessage());
            }
        }

        if (getProperty("hub.read.only", false)) {
            ensureReadOnlyPropertiesAreSet();
        }
    }

    private void ensureReadOnlyPropertiesAreSet() {
        properties.put("webhook.leadership.enabled", "false");
        properties.put("replication.enabled", "false");
        properties.put("s3.batch.management.enabled", "false");
        properties.put("s3.config.management.enabled", "false");
        properties.putIfAbsent("channel.latest.update.svc.enabled", "false");
        properties.put("s3Verifier.run", "false");
    }

    private void setDefaultProperties() {
        log.warn("unable to load files, using baked in defaults");

        final Properties propertiesDefault = new Properties();
        propertiesDefault.put("hub.type", "aws");
        propertiesDefault.put("app.name", "hub-v2");
        propertiesDefault.put("dynamo.endpoint", "dynamodb.us-east-1.amazonaws.com");
        propertiesDefault.put("app.environment", "local");
        propertiesDefault.put("s3.environment", "local");
        propertiesDefault.put("s3.endpoint", "s3-external-1.amazonaws.com");
        propertiesDefault.put("s3.writeQueueSize", "2000");
        propertiesDefault.put("dynamo.table_creation_wait_minutes", "10");
        propertiesDefault.put("app.lib_path", "");
        propertiesDefault.put("app.shutdown_delay_seconds", "2");
        propertiesDefault.put("app.url", "http://localhost:9080/");
        propertiesDefault.put("spoke.path", "/tmp/spoke/test");
        propertiesDefault.put("spoke.ttlMinutes", "240");
        propertiesDefault.put("http.bind_port", "9080");
        propertiesDefault.put("hosted_graphite.enable", "false");
        propertiesDefault.put("zookeeper.connection", "localhost:2181");
        propertiesDefault.put("runSingleZookeeperInternally", "singleNode");
        propertiesDefault.put("hub.protect.channels", "false");
        propertiesDefault.put("metrics.enable", "false");
        propertiesDefault.put("s3Verifier.run", "false");
        propertiesDefault.put("aws.signing_region", "us-east-1");

        this.properties = propertiesDefault;
    }
}

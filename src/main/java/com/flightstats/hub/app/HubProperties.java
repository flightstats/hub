package com.flightstats.hub.app;

import com.flightstats.hub.spoke.SpokeStore;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Properties;

@Slf4j
public class HubProperties {

    @Getter
    private final Properties properties;

    public HubProperties(Properties properties) {
        this.properties = properties;
    }

    public boolean shouldRunZooKeeperInProcess() {
        String property = getProperty("runSingleZookeeperInternally", "");
        return !StringUtils.isEmpty(property);
    }

    public boolean isReadOnly() {
        String readOnlyNodes = getProperty("hub.read.only", "");
        return Arrays.asList(readOnlyNodes.split(","))
                .contains(HubHost.getLocalName());
    }

    public String getAppUrl() {
        return StringUtils.appendIfMissing(getProperty("app.url", ""), "/");
    }

    public boolean isAppEncrypted() {
        return getProperty("app.encrypted", false);
    }

    public int getSpokeTtlMinutes(SpokeStore spokeStore) {
        String property = "spoke." + spokeStore + ".ttlMinutes";
        String fallbackProperty = "spoke.ttlMinutes";
        int defaultTTL = 60;
        return getProperty(property, getProperty(fallbackProperty, defaultTTL));
    }

    public String getAppEnv() {
        return (getProperty("app.name", "hub") + "_" + getProperty("app.environment", "unknown")).replace("-", "_");
    }

    public boolean isProtected() {
        return getProperty("hub.protect.channels", true);
    }

    public String getSpokePath(SpokeStore spokeStore) {
        String property = "spoke." + spokeStore + ".path";
        String fallbackProperty = "spoke.path";
        String defaultPath = "/spoke/" + spokeStore;
        return getProperty(property, getProperty(fallbackProperty, defaultPath));
    }

    public long getLargePayload() {
        return getProperty("app.large.payload.MB", 40) * 1024 * 1024;
    }

    public int getCallbackTimeoutMin() {
        return getProperty("webhook.callbackTimeoutSeconds.min", 1);
    }

    public int getCallbackTimeoutMax() {
        return getProperty("webhook.callbackTimeoutSeconds.max", 1800);
    }

    public int getCallbackTimeoutDefault() {
        return getProperty("webhook.callbackTimeoutSeconds.default", 120);
    }

    public String getSigningRegion() {
        return getProperty("aws.signing_region", "us-east-1");
    }

    public int getS3WriteQueueSize() {
        return getProperty("s3.writeQueueSize", 40000);
    }

    public int getS3WriteQueueThreads() {
        return getProperty("s3.writeQueueThreads", 20);
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

    public String getProperty(String name) {
        return getProperty(name, "");
    }

    public String getProperty(String name, String defaultValue) {
        return properties.getProperty(name, defaultValue);
    }

    public void setProperty(String key, String value) {
        properties.put(key, value);
    }
}

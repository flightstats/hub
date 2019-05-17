package com.flightstats.hub.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;

@Slf4j
public class AppProperties {

    private final PropertiesLoader propertiesLoader;

    @Inject
    public AppProperties(PropertiesLoader propertiesLoader) {
        this.propertiesLoader = propertiesLoader;
    }

    public String getEnv() {
        return propertiesLoader.getProperty("app.environment", "test");
    }

    public String getAppName() {
        return propertiesLoader.getProperty("app.name", "hub");
    }

    public String getAppEnv() {
        return (getAppName() + "_" + getEnv()).replace("-", "_");
    }

    public String getAppLibPath() {
        return propertiesLoader.getProperty(" app.lib_path", "");
    }

    public String getAppUrl() {
        return StringUtils.appendIfMissing(propertiesLoader.getProperty("app.url", ""), "/");
    }

    public String getAppRemoteTimeFile() {
        return propertiesLoader.getProperty("app.remoteTimeFile", "/home/hub/remoteTime");
    }

    public String getAppBirthday() {
        return propertiesLoader.getProperty("app.birthDay", "2015/01/01");
    }

    public boolean isAppEncrypted() {
        return propertiesLoader.getProperty("app.encrypted", false);
    }

    public boolean isReadOnly() {
        return propertiesLoader.getProperty("hub.read.only", false);
    }

    public int getMinPostTimeMillis() {
        return propertiesLoader.getProperty("app.minPostTimeMillis", 5);
    }

    public int getMaxPostTimeMillis() {
        return propertiesLoader.getProperty("app.maxPostTimeMillis", 1000);
    }

    public boolean isNtpMonitorEnabled() {
        return propertiesLoader.getProperty("app.runNtpMonitor", true);
    }

    public int getShutdownWaitTimeInMillis() {
        return propertiesLoader.getProperty("app.shutdown_wait_seconds", 180) * 1000;
    }

    public int getShutdownDelayInMiilis() {
        return propertiesLoader.getProperty("app.shutdown_delay_seconds", 60) * 1000;
    }

    public String getClusterLocation() {
        return propertiesLoader.getProperty("cluster.location", "local");
    }

    public String getHubType() {
        return propertiesLoader.getProperty("hub.type", "aws");
    }

    public int getLogSlowTracesInSec() {
        return propertiesLoader.getProperty("logSlowTracesSeconds", 10) * 1000;
    }

    public int getTracesLimit() {
        return propertiesLoader.getProperty("traces.limit", 50);
    }

    public String getLastContentPathTracing() {
        return propertiesLoader.getProperty("LastContentPathTracing", "channelToTrace");
    }

    public String getKeyStorePasswordPath() {
        return propertiesLoader.getProperty("app.keyStorePasswordPath", "/etc/ssl/key");
    }

    public String getKeyStorePath() {
        return propertiesLoader.getProperty("app.keyStorePath", "/etc/ssl/");
    }

}

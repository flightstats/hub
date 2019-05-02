package com.flightstats.hub.config;

import com.flightstats.hub.app.HubHost;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;

@Slf4j
public class AppProperties {

    private final PropertiesLoader propertiesLoader;

    @Inject
    public AppProperties(PropertiesLoader propertiesLoader) {
        this.propertiesLoader = propertiesLoader;
    }

    public String getEnv() {
        return this.propertiesLoader.getProperty("app.environment", "test");
    }

    public String getAppName() {
        return this.propertiesLoader.getProperty("app.name", "hub");
    }

    public String getAppEnv() {
        return (getAppName() + "_" + getEnv()).replace("-", "_");
    }

    public String getAppUrl() {
        return StringUtils.appendIfMissing(this.propertiesLoader.getProperty("app.url", ""), "/");
    }

    public String getAppRemoteTimeFile() {
        return this.propertiesLoader.getProperty("app.remoteTimeFile", "/home/hub/remoteTime");
    }

    public String getAppBirthday() {
        return this.propertiesLoader.getProperty("app.birthDay", "2015/01/01");
    }

    public boolean isAppEncrypted() {
        return this.propertiesLoader.getProperty("app.encrypted", false);
    }

    public boolean isReadOnly() {
        String readOnlyNodes = this.propertiesLoader.getProperty("hub.read.only", "");
        return Arrays.asList(readOnlyNodes.split(","))
                .contains(HubHost.getLocalName());
    }

    public int getMinPostTimeMillis() {
        return this.propertiesLoader.getProperty("app.minPostTimeMillis", 5);
    }

    public int getMaxPostTimeMillis() {
        return this.propertiesLoader.getProperty("app.maxPostTimeMillis", 1000);
    }

    public boolean isNtpMonitorEnabled() {
        return this.propertiesLoader.getProperty("app.runNtpMonitor", true);
    }

    public int getShutdownWaitTimeInMillis() {
        return this.propertiesLoader.getProperty("app.shutdown_wait_seconds", 180) * 1000;
    }

    public int getShutdownDelayInMiilis() {
        return this.propertiesLoader.getProperty("app.shutdown_delay_seconds", 60) * 1000;
    }

    public String getClusterLocation() {
        return this.propertiesLoader.getProperty("cluster.location", "local");
    }

    public String getHubType() {
        return this.propertiesLoader.getProperty("hub.type", "aws");
    }

    public int getLogSlowTracesInSec() {
        return this.propertiesLoader.getProperty("logSlowTracesSeconds", 10) * 1000;
    }

    public int getTracesLimit() {
        return this.propertiesLoader.getProperty("traces.limit", 50);
    }

    public String getLastContentPathTracing() {
        return this.propertiesLoader.getProperty("LastContentPathTracing", "channelToTrace");
    }

    public String getKeyStorePasswordPath() {
        return this.propertiesLoader.getProperty("app.keyStorePasswordPath", "/etc/ssl/key");
    }

    public String getKeyStorePath() {
        return this.propertiesLoader.getProperty( "app.keyStorePath", "/etc/ssl/");
    }

}

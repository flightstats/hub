package com.flightstats.hub.config;

import com.flightstats.hub.app.HubHost;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;

@Slf4j
public class AppProperty {

    private PropertyLoader propertyLoader;

    @Inject
    public AppProperty(PropertyLoader propertyLoader) {
        this.propertyLoader = propertyLoader;
    }

    public String getEnv() {
        return this.propertyLoader.getProperty("app.environment", "local");
    }

    public String getAppName() {
        return this.propertyLoader.getProperty("app.name", "hub");
    }

    public String getAppEnv() {
        return (getAppName() + "_" + getEnv()).replace("-", "_");
    }

    public String getAppUrl() {
        return StringUtils.appendIfMissing(this.propertyLoader.getProperty("app.url", ""), "/");
    }

    public String getAppRemoteTimeFle() {
        return this.propertyLoader.getProperty("app.remoteTimeFile", "/home/hub/remoteTime");
    }

    public String getAppBirthday() {
        return this.propertyLoader.getProperty("app.birthDay", "2015/01/01");
    }

    public boolean isAppEncrypted() {
        return this.propertyLoader.getProperty("app.encrypted", false);
    }

    public boolean isProtected() {
        return this.propertyLoader.getProperty("hub.protect.channels", true);
    }

    public boolean isReadOnly() {
        String readOnlyNodes = this.propertyLoader.getProperty("hub.read.only", "");
        return Arrays.asList(readOnlyNodes.split(","))
                .contains(HubHost.getLocalName());
    }

    public int getStableSeconds() {
        return this.propertyLoader.getProperty("app.stable_seconds", 5);
    }

    public int getQueryMergeMaxWaitInMins() {
        return this.propertyLoader.getProperty("query.merge.max.wait.minutes", 2);
    }

    public int getMaxPayloadSizeInMB() {
        return this.propertyLoader.getProperty("app.maxPayloadSizeMB", 40);
    }

    public long getLargePayload() {
        return this.propertyLoader.getProperty("app.large.payload.MB", 40) * 1024 * 1024;
    }

    public int getDirectionCountLimit() {
        return this.propertyLoader.getProperty("app.directionCountLimit", 10000);
    }

    public int getGCSchedulerDelayInMinutes() {
        return this.propertyLoader.getProperty("hub.gcMinutes", 60);
    }

    public boolean isRunGC() {
        return this.propertyLoader.getProperty("hub.runGC", false);
    }

    public int getMinPostTimeMillis() {
        return this.propertyLoader.getProperty("app.minPostTimeMillis", 5);
    }

    public int getMaxPostTimeMillis() {
        return this.propertyLoader.getProperty("app.maxPostTimeMillis", 1000);
    }

    public boolean isRunNtpMonitor() {
        return this.propertyLoader.getProperty("app.runNtpMonitor", true);
    }

    public int getShutdownWaitTimeInMillis() {
        return this.propertyLoader.getProperty("app.shutdown_wait_seconds", 180) * 1000;
    }

    public int getShutdownDelayInMiilis() {
        return this.propertyLoader.getProperty("app.shutdown_delay_seconds", 60) * 1000;
    }

    public String getClusterLocation() {
        return this.propertyLoader.getProperty("cluster.location", "local");
    }

    public String getHubType() {
        return this.propertyLoader.getProperty("hub.type", "aws");
    }

    public int getHttpConnectTimeoutInSec() {
        return this.propertyLoader.getProperty("http.connect.timeout.seconds", 30);
    }

    public int getHttpReadTimeoutInSec() {
        return this.propertyLoader.getProperty("http.read.timeout.seconds", 120);
    }

    public int getHttpMaxRetries() {
        return this.propertyLoader.getProperty("http.maxRetries", 8);
    }

    public int getHttpSleep() {
        return this.propertyLoader.getProperty("http.sleep", 1000);
    }

    public int getHttpBindPort() {
        return this.propertyLoader.getProperty("http.bind_port", 8080);
    }

    public int getLogSlowTracesInSec() {
        return this.propertyLoader.getProperty("logSlowTracesSeconds", 10) * 1000;
    }

    public int getTracesLimit() {
        return this.propertyLoader.getProperty("traces.limit", 50);
    }

    public String getLastContentPathTracing() {
        return this.propertyLoader.getProperty("LastContentPathTracing", "channelToTrace");
    }
}

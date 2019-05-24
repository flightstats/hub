package com.flightstats.hub.config.properties;

import javax.inject.Inject;

public class SystemProperties {

    private final PropertiesLoader propertiesLoader;

    @Inject
    public SystemProperties(PropertiesLoader propertiesLoader) {
        this.propertiesLoader = propertiesLoader;
    }

    public int getGcSchedulerDelayInMinutes() {
        return propertiesLoader.getProperty("hub.gcMinutes", 60);
    }

    public boolean isGcEnabled() {
        return propertiesLoader.getProperty("hub.runGC", false);
    }

    public int getHttpConnectTimeoutInSec() {
        return propertiesLoader.getProperty("http.connect.timeout.seconds", 30);
    }

    public int getHttpReadTimeoutInSec() {
        return propertiesLoader.getProperty("http.read.timeout.seconds", 120);
    }

    public int getHttpMaxRetries() {
        return propertiesLoader.getProperty("http.maxRetries", 8);
    }

    public int getHttpSleep() {
        return propertiesLoader.getProperty("http.sleep", 1000);
    }

    public int getHttpBindPort() {
        return propertiesLoader.getProperty("http.bind_port", 8080);
    }

    public String getHttpBindIp() {
        return propertiesLoader.getProperty(    "http.bind_ip", "0.0.0.0");
    }

    public int getHttpIdleTimeInMillis() {
        return propertiesLoader.getProperty(    "http.idle_timeout", 30 * 1000);
    }

}

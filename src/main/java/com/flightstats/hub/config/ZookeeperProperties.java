package com.flightstats.hub.config;

import javax.inject.Inject;

public class ZookeeperProperties {

    private final PropertiesLoader propertiesLoader;

    @Inject
    public ZookeeperProperties(PropertiesLoader propertiesLoader) {
        this.propertiesLoader = propertiesLoader;
    }

    public String getConnection() {
        return this.propertiesLoader.getProperty("zookeeper.connection", "localhost:2181");
    }

    public int getBaseSleepTimeInMillis() {
        return this.propertiesLoader.getProperty("zookeeper.baseSleepTimeMs", 10);
    }

    public int getMaxSleepTimeInMillis() {
        return this.propertiesLoader.getProperty("zookeeper.maxSleepTimeMs", 10000);
    }

    public int getMaxRetries() {
        return this.propertiesLoader.getProperty("zookeeper.maxRetries", 20);
    }

    public int getWatchManagerThreadCount() {
        return this.propertiesLoader.getProperty("watchManager.threads", 10);
    }

}
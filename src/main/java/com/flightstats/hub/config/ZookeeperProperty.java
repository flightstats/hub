package com.flightstats.hub.config;

import javax.inject.Inject;

public class ZookeeperProperty {

    private PropertyLoader propertyLoader;

    @Inject
    public ZookeeperProperty(PropertyLoader propertyLoader) {
        this.propertyLoader = propertyLoader;
    }

    public String getConnection() {
        return this.propertyLoader.getProperty("zookeeper.connection", "localhost:2181");
    }

    public int getBaseSleepTimeInMillis() {
        return this.propertyLoader.getProperty("zookeeper.baseSleepTimeMs", 10);
    }

    public int getMaxSleepTimeInMillis() {
        return this.propertyLoader.getProperty("zookeeper.maxSleepTimeMs", 10000);
    }

    public int getMaxRetries() {
        return this.propertyLoader.getProperty("zookeeper.maxRetries", 20);
    }

    public int getWatchManagerThreadCount() {
        return this.propertyLoader.getProperty("watchManager.threads", 10);
    }

}
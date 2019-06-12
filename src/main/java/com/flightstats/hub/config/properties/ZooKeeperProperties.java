package com.flightstats.hub.config.properties;

import javax.inject.Inject;

public class ZooKeeperProperties {

    private final PropertiesLoader propertiesLoader;

    @Inject
    public ZooKeeperProperties(PropertiesLoader propertiesLoader) {
        this.propertiesLoader = propertiesLoader;
    }

    public String getConnection() {
        return propertiesLoader.getProperty("zookeeper.connection", "localhost:2181");
    }

    public int getBaseSleepTimeInMillis() {
        return propertiesLoader.getProperty("zookeeper.baseSleepTimeMs", 10);
    }

    public int getMaxSleepTimeInMillis() {
        return propertiesLoader.getProperty("zookeeper.maxSleepTimeMs", 10000);
    }

    public int getMaxRetries() {
        return propertiesLoader.getProperty("zookeeper.maxRetries", 20);
    }

    public int getWatchManagerThreadCount() {
        return propertiesLoader.getProperty("watchManager.threads", 10);
    }

    public boolean isSingleServerModeEnabled() {
        String runMode = propertiesLoader.getProperty("runSingleZookeeperInternally", "");
        if ("singleNode".equals(runMode)) {
            return true;
        }
        return false;
    }

}
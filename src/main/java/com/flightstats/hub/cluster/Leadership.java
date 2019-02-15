package com.flightstats.hub.cluster;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

public class Leadership {
    private static final Logger logger = LoggerFactory.getLogger(Leadership.class);
    private ZooKeeperState zooKeeperState;

    private final AtomicBoolean hasLeadership = new AtomicBoolean(false);

    Leadership(ZooKeeperState zooKeeperState) {
        this.zooKeeperState = zooKeeperState;
    }

    public boolean hasLeadership() {
        logger.trace("hasLeadership : shouldKeepWorking() {} , hasLeadership.get() {}", zooKeeperState.shouldKeepWorking(), hasLeadership.get());
        return zooKeeperState.shouldKeepWorking() && hasLeadership.get();
    }

    public void close() {
        setLeadership(false);
    }

    public void setLeadership(boolean leadership) {
        hasLeadership.set(leadership);
        logger.trace("setLeadership : shouldKeepWorking() {} , hasLeadership.get() {}", zooKeeperState.shouldKeepWorking(), hasLeadership.get());
    }

}

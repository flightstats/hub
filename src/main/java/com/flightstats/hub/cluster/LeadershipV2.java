package com.flightstats.hub.cluster;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

public class LeadershipV2 implements Leadership {
    private static final Logger logger = LoggerFactory.getLogger(LeadershipV2.class);
    private final AtomicBoolean hasLeadership = new AtomicBoolean(false);
    private ZooKeeperState zooKeeperState;

    LeadershipV2(ZooKeeperState zooKeeperState) {
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

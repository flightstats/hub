package com.flightstats.hub.cluster;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class Leadership {
    private ZooKeeperState zooKeeperState;

    private final AtomicBoolean hasLeadership = new AtomicBoolean(false);

    Leadership(ZooKeeperState zooKeeperState) {
        this.zooKeeperState = zooKeeperState;
    }

    public boolean hasLeadership() {
        log.trace("hasLeadership : shouldKeepWorking() {} , hasLeadership.get() {}", zooKeeperState.shouldKeepWorking(), hasLeadership.get());
        return zooKeeperState.shouldKeepWorking() && hasLeadership.get();
    }

    public void close() {
        setLeadership(false);
    }

    public void setLeadership(boolean leadership) {
        hasLeadership.set(leadership);
        log.trace("setLeadership : shouldKeepWorking() {} , hasLeadership.get() {}", zooKeeperState.shouldKeepWorking(), hasLeadership.get());
    }

}

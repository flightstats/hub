package com.flightstats.hub.cluster;

import java.util.concurrent.atomic.AtomicBoolean;

public class LeadershipV2 implements Leadership {
    private ZooKeeperState zooKeeperState;
    private AtomicBoolean alive = new AtomicBoolean(true);
    private AtomicBoolean hasLeadership = new AtomicBoolean(false);

    public LeadershipV2(ZooKeeperState zooKeeperState) {
        this.zooKeeperState = zooKeeperState;
    }

    public boolean hasLeadership() {
        return zooKeeperState.shouldKeepWorking() && hasLeadership.get();
    }

    //todo - gfm - do we need this?
    public void close() {
        alive.set(false);
    }

    public void setLeadership(boolean leadership) {
        if (alive.get()) {
            hasLeadership.set(leadership);
        }
    }

}

package com.flightstats.hub.cluster;

import java.util.concurrent.atomic.AtomicBoolean;

public class LeadershipV1 implements Leadership {
    private AtomicBoolean alive = new AtomicBoolean(true);
    private AtomicBoolean hasLeadership = new AtomicBoolean(false);

    @Override
    public boolean hasLeadership() {
        return alive.get() && hasLeadership.get();
    }

    @Override
    public void close() {
        alive.set(false);
    }

    @Override
    public void setLeadership(boolean leadership) {
        if (alive.get()) {
            hasLeadership.set(leadership);
        }
    }

}

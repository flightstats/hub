package com.flightstats.hub.cluster;

import java.util.concurrent.atomic.AtomicBoolean;

public class Leadership {
    private AtomicBoolean alive = new AtomicBoolean(true);
    private AtomicBoolean hasLeadership = new AtomicBoolean(false);

    public boolean hasLeadership() {
        return alive.get() && hasLeadership.get();
    }

    public void close() {
        alive.set(false);
    }

    public void setLeadership(boolean leadership) {
        if (alive.get()) {
            hasLeadership.set(leadership);
        }
    }

}

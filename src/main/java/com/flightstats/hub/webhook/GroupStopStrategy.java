package com.flightstats.hub.webhook;

import com.github.rholder.retry.Attempt;
import com.github.rholder.retry.StopStrategy;

import java.util.concurrent.atomic.AtomicBoolean;

class GroupStopStrategy implements StopStrategy {
    private final AtomicBoolean hasLeadership;

    GroupStopStrategy(AtomicBoolean hasLeadership) {
        this.hasLeadership = hasLeadership;
    }

    @Override
    public boolean shouldStop(Attempt failedAttempt) {
        if (!hasLeadership.get()) {
            return true;
        }
        return false;
    }
}

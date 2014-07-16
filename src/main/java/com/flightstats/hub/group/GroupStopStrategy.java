package com.flightstats.hub.group;

import com.github.rholder.retry.StopStrategy;

import java.util.concurrent.atomic.AtomicBoolean;

public class GroupStopStrategy implements StopStrategy {
    private final AtomicBoolean hasLeadership;

    public GroupStopStrategy(AtomicBoolean hasLeadership) {
        this.hasLeadership = hasLeadership;
    }

    @Override
    public boolean shouldStop(int previousAttemptNumber, long delaySinceFirstAttemptInMillis) {
        return !hasLeadership.get();
    }
}

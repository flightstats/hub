package com.flightstats.hub.group;

import com.github.rholder.retry.Attempt;
import com.github.rholder.retry.StopStrategy;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class GroupStopStrategy implements StopStrategy {
    private final AtomicBoolean hasLeadership;
    private final Group group;

    public GroupStopStrategy(AtomicBoolean hasLeadership, Group group) {
        this.hasLeadership = hasLeadership;
        this.group = group;
    }

    @Override
    public boolean shouldStop(Attempt failedAttempt) {
        if (!hasLeadership.get()) {
            return true;
        }
        return reachedStopStrategy(failedAttempt, group);
    }

    public static boolean reachedStopStrategy(Attempt failedAttempt, Group group) {
        if (group.isNeverStop()) {
            return false;
        } else if (group.isTTL()) {
            Integer ttlMinutes = group.getTtlMinutes();
            long maxDelay = TimeUnit.MILLISECONDS.convert(ttlMinutes, TimeUnit.MINUTES);
            if (failedAttempt.getDelaySinceFirstAttempt() >= maxDelay) {
                return true;
            }
        }
        return false;
    }
}

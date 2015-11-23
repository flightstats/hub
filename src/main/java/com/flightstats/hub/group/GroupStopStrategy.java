package com.flightstats.hub.group;

import com.github.rholder.retry.Attempt;
import com.github.rholder.retry.StopStrategy;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class GroupStopStrategy implements StopStrategy {
    private static long ttlMillis;
    private final AtomicBoolean hasLeadership;
    private final Group group;

    public GroupStopStrategy(AtomicBoolean hasLeadership, Group group) {
        this.hasLeadership = hasLeadership;
        this.group = group;
        ttlMillis = TimeUnit.MILLISECONDS.convert(group.getTtlMinutes(), TimeUnit.MINUTES);
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
            if (failedAttempt.getDelaySinceFirstAttempt() >= ttlMillis) {
                return true;
            }
        }
        return false;
    }
}

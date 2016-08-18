package com.flightstats.hub.webhook;

import com.github.rholder.retry.Attempt;
import com.github.rholder.retry.StopStrategy;

import java.util.concurrent.atomic.AtomicBoolean;

class WebhookStopStrategy implements StopStrategy {
    private final AtomicBoolean hasLeadership;

    WebhookStopStrategy(AtomicBoolean hasLeadership) {
        this.hasLeadership = hasLeadership;
    }

    @Override
    public boolean shouldStop(Attempt failedAttempt) {
        return !hasLeadership.get();
    }
}

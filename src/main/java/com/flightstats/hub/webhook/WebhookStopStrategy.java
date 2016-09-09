package com.flightstats.hub.webhook;

import com.flightstats.hub.cluster.Leadership;
import com.github.rholder.retry.Attempt;
import com.github.rholder.retry.StopStrategy;

class WebhookStopStrategy implements StopStrategy {
    private final Leadership leadership;

    WebhookStopStrategy(Leadership leadership) {
        this.leadership = leadership;
    }

    @Override
    public boolean shouldStop(Attempt failedAttempt) {
        return !leadership.hasLeadership();
    }
}

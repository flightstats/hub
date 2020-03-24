package com.flightstats.hub.webhook;

import com.flightstats.hub.config.properties.LocalHostProperties;
import com.flightstats.hub.config.properties.WebhookProperties;
import javax.inject.Inject;
import com.google.inject.Singleton;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.Wither;

import java.util.Set;

import static java.util.stream.Collectors.toSet;

@Singleton
public class WebhookLeaderState {
    private final WebhookLeaderLocks webhookLeaderLocks;
    private final LocalHostProperties localHostProperties;

    @Inject
    public WebhookLeaderState(
            WebhookLeaderLocks webhookLeaderLocks,
            ActiveWebhookSweeper activeWebhookSweeper,
            WebhookProperties webhookProperties,
            LocalHostProperties localHostProperties) {
        this.webhookLeaderLocks = webhookLeaderLocks;

        if (webhookProperties.isWebhookLeadershipEnabled()) {
            activeWebhookSweeper.cleanupEmpty();
        }
        this.localHostProperties = localHostProperties;
    }

    public RunningState getState(String webhookName) {
        return RunningState.builder()
                .leadershipAcquired(hasLeader(webhookName))
                .runningServers(getServers(webhookName))
                .build();
    }

    private boolean hasLeader(String webhookName) {
        return webhookLeaderLocks.getWebhooks().contains(webhookName);
    }

    private Set<String> getServers(String name) {
        return webhookLeaderLocks.getServerLeases(name).stream()
                .map(server -> server + ":" + localHostProperties.getPort())
                .collect(toSet());
    }

    @Builder
    @Wither
    @Value
    public static class RunningState {
        boolean leadershipAcquired;
        Set<String> runningServers;

        public boolean isRunningOnSingleServer() {
            return isLeadershipAcquired() && getRunningServers().size() == 1;
        }

        public boolean isStopped() {
            return !isLeadershipAcquired() && getRunningServers().isEmpty();
        }

        public boolean isRunningInAbnormalState() {
            return !isRunningOnSingleServer() && !isStopped();
        }
    }
}

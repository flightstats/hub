package com.flightstats.hub.webhook;

import com.flightstats.hub.config.properties.LocalHostProperties;
import com.flightstats.hub.config.properties.WebhookProperties;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.util.Set;

import static java.util.stream.Collectors.toSet;

@Singleton
public class ActiveWebhooks {
    private final WebhookLeaderLocks webhookLeaderLocks;
    private final LocalHostProperties localHostProperties;

    @Inject
    public ActiveWebhooks(
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

    boolean isActiveWebhook(String webhookName) {
        return webhookLeaderLocks.getWebhooks().contains(webhookName);
    }

    public Set<String> getServers(String name) {
        return webhookLeaderLocks.getServerLeases(name).stream()
                .map(server -> server + ":" + localHostProperties.getPort())
                .collect(toSet());
    }
}

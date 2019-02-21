package com.flightstats.hub.webhook;

import com.flightstats.hub.app.HubHost;
import com.google.common.util.concurrent.AbstractScheduledService;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.flightstats.hub.app.HubServices.register;
import static java.util.stream.Collectors.toSet;

@Singleton
public class ActiveWebhooks {
    private final WebhookLeaderLocks webhookLeaderLocks;

    @Inject
    public ActiveWebhooks(WebhookLeaderLocks webhookLeaderLocks, ActiveWebhookSweeper activeWebhookSweeper) {
        this.webhookLeaderLocks = webhookLeaderLocks;

        activeWebhookSweeper.cleanupEmpty();
    }

    public boolean isActiveWebhook(String webhookName) {
        return webhookLeaderLocks.getWebhooks().contains(webhookName);
    }

    public Set<String> getServers(String name) {
        return webhookLeaderLocks.getServerLeases(name).stream()
                .map(server -> server + ":" + HubHost.getLocalPort())
                .collect(toSet());
    }
}

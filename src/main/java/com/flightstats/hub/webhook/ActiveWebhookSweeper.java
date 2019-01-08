package com.flightstats.hub.webhook;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public class ActiveWebhookSweeper {
    private static final Logger logger = LoggerFactory.getLogger(ActiveWebhookSweeper.class);
    private final WebhookLeaderLocks webhookLeaderLocks;

    @Inject
    public ActiveWebhookSweeper(WebhookLeaderLocks webhookLeaderLocks) {
        this.webhookLeaderLocks = webhookLeaderLocks;
    }

    void cleanupEmpty() {
        logger.info("cleaning empty webhook leader nodes...");

        Set<String> currentData = webhookLeaderLocks.getWebhooks();
        logger.info("data {}", currentData.size());

        currentData.stream()
                .filter(this::isEmpty)
                .forEach(this::deleteWebhookLeader);
    }

    private boolean isEmpty(String webhookName) {
        return Stream.of(webhookLeaderLocks.getLeasePaths(webhookName), webhookLeaderLocks.getLockPaths(webhookName))
                .allMatch(List::isEmpty);
    }

    private void deleteWebhookLeader(String webhookName) {
        logger.info("deleting empty {}", webhookName);
        webhookLeaderLocks.deleteWebhookLeader(webhookName);

    }

}

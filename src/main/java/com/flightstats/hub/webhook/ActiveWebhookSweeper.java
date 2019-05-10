package com.flightstats.hub.webhook;

import com.flightstats.hub.metrics.StatsdReporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

public class ActiveWebhookSweeper {
    private static final Logger logger = LoggerFactory.getLogger(ActiveWebhookSweeper.class);
    private final WebhookLeaderLocks webhookLeaderLocks;
    private final StatsdReporter statsdReporter;

    @Inject
    public ActiveWebhookSweeper(WebhookLeaderLocks webhookLeaderLocks,
                                StatsdReporter statsdReporter) {
        this.webhookLeaderLocks = webhookLeaderLocks;
        this.statsdReporter = statsdReporter;
    }

    void cleanupEmpty() {
        logger.info("cleaning empty webhook leader nodes...");

        Set<String> currentData = webhookLeaderLocks.getWebhooks();
        logger.info("data {}", currentData.size());

        List<String> emptyWebhookLeaders = currentData.stream()
                .filter(this::isEmpty)
                .collect(toList());

        emptyWebhookLeaders.forEach(this::deleteWebhookLeader);
        statsdReporter.count("webhook.leaders.cleanup", emptyWebhookLeaders.size());
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

package com.flightstats.hub.webhook;

import com.flightstats.hub.metrics.StatsdReporter;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

@Slf4j
public class ActiveWebhookSweeper {
    private final WebhookLeaderLocks webhookLeaderLocks;
    private final StatsdReporter statsdReporter;

    @Inject
    public ActiveWebhookSweeper(WebhookLeaderLocks webhookLeaderLocks,
                                StatsdReporter statsdReporter) {
        this.webhookLeaderLocks = webhookLeaderLocks;
        this.statsdReporter = statsdReporter;
    }

    void cleanupEmpty() {
        log.info("cleaning empty webhook leader nodes...");

        Set<String> currentData = webhookLeaderLocks.getWebhooks();
        log.info("data {}", currentData.size());

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
        log.info("deleting empty webhook leader {}", webhookName);
        webhookLeaderLocks.deleteWebhookLeader(webhookName);

    }

}

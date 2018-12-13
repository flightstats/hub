package com.flightstats.hub.webhook;

import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public class ActiveWebhookSweeper {
    private static final Logger logger = LoggerFactory.getLogger(WebhookLeaderServers.class);
    private final WebhookLeaderServers webhookLeaderServers;

    @Inject
    public ActiveWebhookSweeper(WebhookLeaderServers webhookLeaderServers) {
        this.webhookLeaderServers = webhookLeaderServers;
    }

    void cleanupEmpty() throws Exception {
        logger.info("cleaning empty webhook leader nodes...");

        Set<String> currentData = webhookLeaderServers.getWebhooks();
        logger.info("data {}", currentData.size());

        currentData.stream()
                .filter(this::isEmpty)
                .forEach(this::deleteWebhookLeader);
    }

    private boolean isEmpty(String webhookName) {
        return Stream.of(webhookLeaderServers.getLeasePaths(webhookName), webhookLeaderServers.getLockPaths(webhookName))
                .allMatch(List::isEmpty);
    }

    @SneakyThrows
    private void deleteWebhookLeader(String webhookName) {
        logger.info("deleting empty {}", webhookName);
        webhookLeaderServers.deleteWebhookLeader(webhookName);

    }

}

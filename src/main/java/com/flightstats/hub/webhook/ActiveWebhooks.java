package com.flightstats.hub.webhook;

import com.flightstats.hub.app.HubHost;
import com.flightstats.hub.util.SafeZooKeeperUtils;
import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.lang3.StringUtils;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

@Singleton
public class ActiveWebhooks {
    private static final Logger logger = LoggerFactory.getLogger(ActiveWebhooks.class);
    private static final String WEBHOOK_LEADER = "/WebhookLeader";
    private static final String LEASE_NODE = "leases";
    private static final String LOCK_NODE = "locks";

    private final SafeZooKeeperUtils zooKeeperUtils;

    private PathChildrenCache webhooks;

    @Inject
    public ActiveWebhooks(SafeZooKeeperUtils safeZooKeeperUtils) throws Exception {
        this.zooKeeperUtils = safeZooKeeperUtils;

        this.webhooks = zooKeeperUtils.initializeCache(WEBHOOK_LEADER);

        logger.info("cleaning...");
        cleanupEmpty();
    }

    @VisibleForTesting
    void cleanupEmpty() {
        logger.info("cleaning empty webhook leader nodes...");

        Set<String> currentData = getWebhooks();
        logger.info("data {}", currentData.size());

        List<String> emptyWebhookLeaders = currentData.stream()
                .filter(this::isEmpty)
                .collect(toList());

        emptyWebhookLeaders.forEach(this::deleteWebhookLeader);
    }

    private Set<String> getWebhooks() {
        return webhooks.getCurrentData().stream()
                .map((childData -> StringUtils.substringAfterLast(childData.getPath(), "/")))
                .collect(toSet());
    }

    boolean isActiveWebhook(String webhookName) {
        return getWebhooks().contains(webhookName);
    }

    public Set<String> getServers(String webhookName) {
        return zooKeeperUtils.getChildren(WEBHOOK_LEADER, webhookName, LEASE_NODE).stream()
                .map(lease -> zooKeeperUtils.getData(WEBHOOK_LEADER, webhookName, LEASE_NODE, lease))
                .flatMap(optional -> optional.map(Stream::of).orElse(Stream.empty()))
                .map(server -> format("%s:%s", server, HubHost.getLocalPort()))
                .collect(toSet());
    }

    private boolean isEmpty(String webhookName) {
        return Stream.of(getLeasePaths(webhookName), getLockPaths(webhookName))
                .allMatch(List::isEmpty);
    }

    private List<String> getLeasePaths(String webhookName) {
        return zooKeeperUtils.getChildren(WEBHOOK_LEADER, webhookName, LEASE_NODE);
    }

    private List<String> getLockPaths(String webhookName) {
        return zooKeeperUtils.getChildren(WEBHOOK_LEADER, webhookName, LOCK_NODE);
    }

    private void deleteWebhookLeader(String webhookName) {
        logger.info("deleting empty {}", webhookName);
        zooKeeperUtils.deletePathAndChildren(WEBHOOK_LEADER, webhookName);
    }
}

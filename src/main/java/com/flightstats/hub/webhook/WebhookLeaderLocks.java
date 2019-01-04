package com.flightstats.hub.webhook;

import com.flightstats.hub.cluster.CuratorLock;
import com.flightstats.hub.util.SafeZooKeeperUtils;
import com.google.inject.Singleton;
import org.apache.commons.lang3.StringUtils;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;

import javax.inject.Inject;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toSet;

@Singleton
public class WebhookLeaderLocks {
    private static final String WEBHOOK_LEADER = "/WebhookLeader";
    private static final String LEASE_NODE = "leases";
    private static final String LOCK_NODE = "locks";
    private final SafeZooKeeperUtils zooKeeperUtils;

    private PathChildrenCache webhooks;

    @Inject
    public WebhookLeaderLocks(SafeZooKeeperUtils zooKeeperUtils) throws Exception {
        this.zooKeeperUtils = zooKeeperUtils;

        webhooks = zooKeeperUtils.initializeCache(WEBHOOK_LEADER);
    }

    CuratorLock createLock(String webhook) {
        return zooKeeperUtils.createLock(WEBHOOK_LEADER, webhook);
    }

    Set<String> getServerLeases(String webhook) {
        return getLeasePaths(webhook).stream()
                .map(lease -> zooKeeperUtils.getData(WEBHOOK_LEADER, webhook, LEASE_NODE, lease))
                .flatMap(optional -> optional.map(Stream::of).orElse(Stream.empty()))
                .collect(toSet());
    }

    Set<String> getWebhooks() {
        return getWebhookCache().stream()
                .map((childData -> StringUtils.substringAfterLast(childData.getPath(), "/")))
                .collect(toSet());
    }

    List<String> getLeasePaths(String webhookName) {
        return zooKeeperUtils.getChildren(WEBHOOK_LEADER, webhookName, LEASE_NODE);
    }

    List<String> getLockPaths(String webhookName) {
        return zooKeeperUtils.getChildren(WEBHOOK_LEADER, webhookName, LOCK_NODE);
    }

    void deleteWebhookLeader(String webhookName) {
        zooKeeperUtils.deletePathAndChildren(WEBHOOK_LEADER, webhookName);
    }

    private List<ChildData> getWebhookCache() {
        return webhooks.getCurrentData();
    }
}

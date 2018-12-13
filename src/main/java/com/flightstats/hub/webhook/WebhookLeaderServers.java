package com.flightstats.hub.webhook;

import com.google.inject.Singleton;
import org.apache.commons.lang3.StringUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toSet;

@Singleton
public class WebhookLeaderServers {
    private static final Logger logger = LoggerFactory.getLogger(WebhookLeaderServers.class);
    private static final String WEBHOOK_LEADER = "/WebhookLeader";
    private static final String LEASE_NODE = "leases";
    private static final String LOCK_NODE = "locks";
    private final CuratorFramework curator;

    private PathChildrenCache webhooks;

    @Inject
    public WebhookLeaderServers(CuratorFramework curator) throws Exception {
        this.curator = curator;

        webhooks = new PathChildrenCache(curator, WEBHOOK_LEADER, true);
        webhooks.start(PathChildrenCache.StartMode.BUILD_INITIAL_CACHE);
    }

    Set<String> getServers(String webhook) {
        return getLeasePaths(webhook).stream()
                .map(lease -> getData(webhook, LEASE_NODE, lease))
                .flatMap(optional -> optional.map(Stream::of).orElse(Stream.empty()))
                .collect(toSet());
    }

    Set<String> getWebhooks() {
        return getWebhookCache().stream()
                .map((childData -> StringUtils.substringAfterLast(childData.getPath(), "/")))
                .collect(toSet());
    }

    List<String> getLeasePaths(String webhookName) {
        return getChildren(webhookName, LEASE_NODE);
    }

    List<String> getLockPaths(String webhookName) {
        return getChildren(webhookName, LOCK_NODE);
    }

    void deleteWebhookLeader(String webhookName) throws Exception {
        curator.delete().deletingChildrenIfNeeded().forPath(WEBHOOK_LEADER + "/" + webhookName);
    }

    private List<ChildData> getWebhookCache() {
        return webhooks.getCurrentData();
    }

    private String getWebhookPath(String webhookName) {
        return WEBHOOK_LEADER + "/" + webhookName + "/";
    }

    private List<String> getChildren(String webhookName, String trailingPath) {
        try {
            String path = getWebhookPath(webhookName) + trailingPath;
            return curator.getChildren().forPath(path);
        } catch (KeeperException.NoNodeException ignore) {
            logger.info("no nodes " + webhookName);
            return emptyList();
        } catch (Exception e) {
            logger.warn("unable to get children " + webhookName, e);
            return emptyList();
        }
    }

    private Optional<String> getData(String webhookName, String lockOrLease, String trailingPath) {
        try {
            String path = getWebhookPath(webhookName) + lockOrLease + "/" + trailingPath;
            byte[] bytes = curator.getData().forPath(path);
            return Optional.of(new String(bytes));
        } catch (KeeperException.NoNodeException ignore) {
            logger.info("no nodes " + webhookName);
            return Optional.empty();
        } catch (Exception e) {
            logger.warn("unable to get data" + webhookName, e);
            return Optional.empty();
        }
    }
}

package com.flightstats.hub.webhook;

import com.flightstats.hub.app.HubHost;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.AbstractScheduledService;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.lang3.StringUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.flightstats.hub.app.HubServices.register;

@Singleton
public class ActiveWebhooks {
    private static final Logger logger = LoggerFactory.getLogger(ActiveWebhooks.class);
    private static final String WEBHOOK_LEADER = "/WebhookLeader";
    private final CuratorFramework curator;

    private PathChildrenCache webhooks;

    @Inject
    public ActiveWebhooks(CuratorFramework curator) throws Exception {
        this.curator = curator;

        webhooks = new PathChildrenCache(curator, WEBHOOK_LEADER, true);
        webhooks.start(PathChildrenCache.StartMode.BUILD_INITIAL_CACHE);

        register(new WebhookLeaderCleanupService());
    }

    public boolean isActiveWebhook(String webhookName) {
        return get(webhooks).contains(webhookName);
    }

    public Set<String> getServers(String name) {
        Set<String> servers = new HashSet<>();
        try {
            addAll(name, servers, "leases");
        } catch (KeeperException.NoNodeException ignore) {
            logger.info("no nodes " + name);
        } catch (Exception e) {
            logger.warn("unable to get children " + name, e);
        }
        logger.debug("{} v2 servers {}", name, servers);
        return servers;
    }
    private Set<String> get(PathChildrenCache webhooks) {
        return webhooks.getCurrentData().stream()
                .map((childData -> StringUtils.substringAfterLast(childData.getPath(), "/")))
                .collect(Collectors.toSet());
    }

    private void addAll(String name, Set<String> servers, String zkName) throws Exception {
        String path = WEBHOOK_LEADER + "/" + name + "/" + zkName;
        List<String> leases = curator.getChildren().forPath(path);
        for (String lease : leases) {
            byte[] bytes = curator.getData().forPath(path + "/" + lease);
            servers.add(new String(bytes) + ":" + HubHost.getLocalPort());
        }
    }

    @VisibleForTesting
    void cleanupEmpty() throws Exception {
        logger.info("cleaning empty webhook leader nodes...");
        cleanupEmpty("/leases", "/locks");
    }

    private void cleanupEmpty(String... trailingPath) throws Exception {
        List<ChildData> currentData = webhooks.getCurrentData();
        logger.info("data {}", currentData.size());
        for (ChildData childData : currentData) {
            boolean isEmpty = true;
            for (String trailing : trailingPath) {
                String fullPath = childData.getPath() + trailing;
                try {
                    List<String> children = curator.getChildren().forPath(fullPath);
                    if (!children.isEmpty()) {
                        isEmpty = false;
                    }
                } catch (KeeperException.NoNodeException ignore) {
                    //ignore
                }
            }
            if (isEmpty) {
                logger.info("deleting empty {}", childData.getPath());
                curator.delete().deletingChildrenIfNeeded().forPath(childData.getPath());
            }
        }
    }

    private class WebhookLeaderCleanupService extends AbstractScheduledService {
        @Override
        protected void runOneIteration() throws Exception {
            cleanupEmpty();
        }

        @Override
        protected Scheduler scheduler() {
            return Scheduler.newFixedRateSchedule(2, 15, TimeUnit.MINUTES);
        }
    }
}

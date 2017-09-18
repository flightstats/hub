package com.flightstats.hub.webhook;

import com.flightstats.hub.app.HubHost;
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
import java.util.stream.Collectors;

@Singleton
public class ActiveWebhooks {

    private static final Logger logger = LoggerFactory.getLogger(ActiveWebhooks.class);
    private static final String V2_LEADER = "/WebhookLeader";
    private static final String V1_LEADER = "/GroupLeader";
    private final CuratorFramework curator;

    //todo - gfm - v1Webhooks can go away eventually
    private PathChildrenCache v1Webhooks;
    private PathChildrenCache v2Webhooks;

    @Inject
    public ActiveWebhooks(CuratorFramework curator) throws Exception {
        this.curator = curator;
        v1Webhooks = new PathChildrenCache(curator, V1_LEADER, true);
        v1Webhooks.start(PathChildrenCache.StartMode.BUILD_INITIAL_CACHE);

        v2Webhooks = new PathChildrenCache(curator, V2_LEADER, true);
        v2Webhooks.start(PathChildrenCache.StartMode.BUILD_INITIAL_CACHE);

        logger.info("cleaning...");
        cleanupEmpty(v1Webhooks, "");
        cleanupEmpty(v2Webhooks, "/leases", "/locks");
    }

    private void cleanupEmpty(PathChildrenCache webhooks, String... trailingPath) throws Exception {
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

    private Set<String> get(PathChildrenCache webhooks) {
        return webhooks.getCurrentData().stream()
                .map((childData -> StringUtils.substringAfterLast(childData.getPath(), "/")))
                .collect(Collectors.toSet());
    }

    Set<String> getV1() {
        return get(v1Webhooks);
    }

    Set<String> getV2() {
        return get(v2Webhooks);
    }

    Set<String> getV2Servers(String name) {
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

    private void addAll(String name, Set<String> servers, String zkName) throws Exception {
        String path = V2_LEADER + "/" + name + "/" + zkName;
        List<String> leases = curator.getChildren().forPath(path);
        for (String lease : leases) {
            byte[] bytes = curator.getData().forPath(path + "/" + lease);
            servers.add(new String(bytes) + ":" + HubHost.getLocalPort());
        }
    }

}

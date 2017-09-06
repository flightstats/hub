package com.flightstats.hub.webhook;

import com.flightstats.hub.app.HubHost;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.lang3.StringUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Singleton
public class ActiveWebhooks {

    private static final Logger logger = LoggerFactory.getLogger(ActiveWebhooks.class);
    private final CuratorFramework curator;

    //todo - gfm - v1Webhooks can go away eventually
    private PathChildrenCache v1Webhooks;
    private PathChildrenCache v2Webhooks;

    @Inject
    public ActiveWebhooks(CuratorFramework curator) throws Exception {
        this.curator = curator;
        v1Webhooks = new PathChildrenCache(curator, "/GroupLeader", true);
        v1Webhooks.start(PathChildrenCache.StartMode.BUILD_INITIAL_CACHE);

        v2Webhooks = new PathChildrenCache(curator, WebhookLeader.LEADER_PATH, true);
        v2Webhooks.start(PathChildrenCache.StartMode.BUILD_INITIAL_CACHE);

        logger.info("cleaning...");
        cleanupEmpty(v1Webhooks);
        cleanupEmpty(v2Webhooks);
    }

    private void cleanupEmpty(PathChildrenCache webhooks) {
        List<ChildData> currentData = webhooks.getCurrentData();
        logger.info("data {}" + currentData.size());
        for (ChildData childData : currentData) {
            String path = childData.getPath();
            logger.info("path {}", path);
            //todo - gfm - we should delete empty nodes.  the hierarchy is different between v1 & v2

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
            addAll(name, servers, "locks");
        } catch (Exception e) {
            logger.warn("unable to get children " + name, e);
        }
        logger.debug("{} v2 servers {}", name, servers);
        return servers;
    }

    private void addAll(String name, Set<String> servers, String zkName) throws Exception {
        String path = WebhookLeader.LEADER_PATH + "/" + name + "/" + zkName;
        List<String> leases = curator.getChildren().forPath(path);
        for (String lease : leases) {
            byte[] bytes = curator.getData().forPath(path + "/" + lease);
            servers.add(new String(bytes) + ":" + HubHost.getLocalPort());
        }
    }

}

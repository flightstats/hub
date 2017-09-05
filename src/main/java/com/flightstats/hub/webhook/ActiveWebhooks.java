package com.flightstats.hub.webhook;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.lang3.StringUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Singleton
public class ActiveWebhooks {

    private static final Logger logger = LoggerFactory.getLogger(ActiveWebhooks.class);

    //todo - gfm - v1Webhooks can go away eventually
    private PathChildrenCache v1Webhooks;
    private PathChildrenCache v2Webhooks;

    @Inject
    public ActiveWebhooks(CuratorFramework curator) throws Exception {
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
            //todo - gfm - do what with empty ???

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

}

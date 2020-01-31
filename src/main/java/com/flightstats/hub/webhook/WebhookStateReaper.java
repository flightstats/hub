package com.flightstats.hub.webhook;

import com.flightstats.hub.cluster.ClusterCacheDao;
import com.flightstats.hub.config.properties.WebhookProperties;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;

import static com.flightstats.hub.constant.ZookeeperNodes.WEBHOOK_LAST_COMPLETED;

@Singleton
@Slf4j
class WebhookStateReaper {
    private final ClusterCacheDao clusterCacheDao;
    private final WebhookContentInFlight contentKeysInFlight;
    private final WebhookErrorService webhookErrorService;
    private final WebhookLeaderLocks webhookLeaderLocks;
    private final WebhookProperties webhookProperties;

    @Inject
    WebhookStateReaper(ClusterCacheDao clusterCacheDao,
                       WebhookContentInFlight contentKeysInFlight,
                       WebhookErrorService webhookErrorService,
                       WebhookLeaderLocks webhookLeaderLocks,
                       WebhookProperties webhookProperties) {
        this.clusterCacheDao = clusterCacheDao;
        this.contentKeysInFlight = contentKeysInFlight;
        this.webhookErrorService = webhookErrorService;
        this.webhookLeaderLocks = webhookLeaderLocks;
        this.webhookProperties = webhookProperties;
    }

    void stop(String webhook) {
        if (!webhookProperties.isWebhookLeadershipEnabled()) {
            return;
        }
        log.debug("stopping {}", webhook);
        webhookLeaderLocks.deleteWebhookLeader(webhook);
        log.info("stopped {}", webhook);
    }

    void delete(String webhook) {
        if (!webhookProperties.isWebhookLeadershipEnabled()) {
            return;
        }
        stop(webhook);
        log.debug("deleting {}", webhook);
        contentKeysInFlight.delete(webhook);
        clusterCacheDao.delete(webhook, WEBHOOK_LAST_COMPLETED);
        webhookErrorService.delete(webhook);
        log.info("deleted {}", webhook);
    }
}

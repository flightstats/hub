package com.flightstats.hub.dao.aws;

import com.flightstats.hub.app.HubServices;
import com.flightstats.hub.config.AppProperties;
import com.flightstats.hub.config.S3Properties;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.replication.S3Batch;
import com.flightstats.hub.util.HubUtils;
import com.flightstats.hub.webhook.ActiveWebhooks;
import com.flightstats.hub.webhook.Webhook;
import com.flightstats.hub.webhook.WebhookService;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;

@Singleton
@Slf4j
public class S3BatchManager {

    private final WebhookService webhookService;
    private final ChannelService channelService;
    private final HubUtils hubUtils;
    private final ActiveWebhooks activeWebhooks;
    private final AppProperties appProperties;

    @Inject
    public S3BatchManager(WebhookService webhookService,
                          ChannelService channelService,
                          HubUtils hubUtils,
                          ActiveWebhooks activeWebhooks,
                          AppProperties appProperties,
                          S3Properties s3Properties) {
        this.webhookService = webhookService;
        this.channelService = channelService;
        this.hubUtils = hubUtils;
        this.activeWebhooks = activeWebhooks;
        this.appProperties = appProperties;

        if (s3Properties.isBatchManagementEnabled()) {
            HubServices.register(new S3BatchManagerService(), HubServices.TYPE.AFTER_HEALTHY_START);
        }
    }

    private void setupBatch() {
        Set<String> existingBatchGroups = new HashSet<>();
        Iterable<Webhook> groups = webhookService.getAllCached();
        for (Webhook webhook : groups) {
            if (S3Batch.isS3BatchCallback(webhook.getName())) {
                existingBatchGroups.add(webhook.getName());
            }
        }
        for (ChannelConfig channel : channelService.getChannels()) {
            S3Batch s3Batch = new S3Batch(channel, hubUtils, appProperties.getAppUrl(), appProperties.getAppEnv());
            if (channel.isSingle()) {
                if (!activeWebhooks.getServers(channel.getName()).isEmpty()) {
                    log.debug("turning off batch webhook {}", channel.getDisplayName());
                    s3Batch.stop();
                }
            } else {
                log.info("batching channel {}", channel.getDisplayName());
                s3Batch.start();
                existingBatchGroups.remove(s3Batch.getGroupName());
            }
        }
        for (String groupName : existingBatchGroups) {
            log.info("stopping unused batch webhook {}", groupName);
            webhookService.delete(groupName);
        }
    }

    private class S3BatchManagerService extends AbstractIdleService {

        @Override
        protected void startUp() {
            Executors.newSingleThreadExecutor().submit(S3BatchManager.this::setupBatch);
        }

        @Override
        protected void shutDown() {
        }
    }

}

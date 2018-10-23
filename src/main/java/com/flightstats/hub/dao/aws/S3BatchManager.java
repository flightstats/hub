package com.flightstats.hub.dao.aws;

import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.app.HubServices;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.replication.S3Batch;
import com.flightstats.hub.util.HubUtils;
import com.flightstats.hub.webhook.ActiveWebhooks;
import com.flightstats.hub.webhook.Webhook;
import com.flightstats.hub.webhook.WebhookService;
import com.google.common.util.concurrent.AbstractIdleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;

@Singleton
public class S3BatchManager {

    private final static Logger logger = LoggerFactory.getLogger(S3BatchManager.class);

    private final WebhookService webhookService;
    private final ActiveWebhooks activeWebhooks;
    private final ChannelService channelService;
    private final HubUtils hubUtils;
    private final HubProperties hubProperties;

    @Inject
    public S3BatchManager(WebhookService webhookService, ActiveWebhooks activeWebhooks, ChannelService channelService, HubUtils hubUtils, HubProperties hubProperties) {
        this.webhookService = webhookService;
        this.activeWebhooks = activeWebhooks;
        this.channelService = channelService;
        this.hubUtils = hubUtils;
        this.hubProperties = hubProperties;

        HubServices.register(new S3BatchManagerService(), HubServices.TYPE.AFTER_HEALTHY_START);
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

    private void setupBatch() {
        Set<String> existingBatchGroups = new HashSet<>();
        Iterable<Webhook> groups = webhookService.getAllCached();
        for (Webhook webhook : groups) {
            if (S3Batch.isS3BatchCallback(webhook.getName())) {
                existingBatchGroups.add(webhook.getName());
            }
        }
        for (ChannelConfig channel : channelService.getChannels()) {
            S3Batch s3Batch = new S3Batch(channel, hubUtils, hubProperties);
            if (channel.isSingle()) {
                if (!activeWebhooks.getServers(channel.getName()).isEmpty()) {
                    logger.debug("turning off batch webhook {}", channel.getDisplayName());
                    s3Batch.stop();
                }
            } else {
                logger.info("batching channel {}", channel.getDisplayName());
                s3Batch.start();
                existingBatchGroups.remove(s3Batch.getGroupName());
            }
        }
        for (String groupName : existingBatchGroups) {
            logger.info("stopping unused batch webhook {}", groupName);
            webhookService.delete(groupName);
        }
    }

}

package com.flightstats.hub.dao.aws;

import com.flightstats.hub.app.HubServices;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.replication.S3Batch;
import com.flightstats.hub.util.HubUtils;
import com.flightstats.hub.webhook.Webhook;
import com.flightstats.hub.webhook.WebhookService;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;

@Singleton
public class S3BatchManager {

    private final static Logger logger = LoggerFactory.getLogger(S3BatchManager.class);

    @Inject
    private WebhookService webhookService;
    @Inject
    private ChannelService channelService;
    @Inject
    private HubUtils hubUtils;

    @Inject
    public S3BatchManager() {
        HubServices.register(new S3BatchManagerService(), HubServices.TYPE.AFTER_HEALTHY_START);
    }

    private class S3BatchManagerService extends AbstractIdleService {

        @Override
        protected void startUp() throws Exception {
            Executors.newSingleThreadExecutor().submit(S3BatchManager.this::setupBatch);
        }

        @Override
        protected void shutDown() throws Exception {
        }
    }

    private void setupBatch() {
        Set<String> existingBatchGroups = new HashSet<>();
        Iterable<Webhook> groups = webhookService.getAll();
        for (Webhook webhook : groups) {
            if (S3Batch.isS3BatchCallback(webhook.getName())) {
                existingBatchGroups.add(webhook.getName());
            }
        }
        for (ChannelConfig channel : channelService.getChannels()) {
            S3Batch s3Batch = new S3Batch(channel, hubUtils);
            if (channel.isSingle()) {
                logger.debug("turning off batch {}", channel.getName());
                s3Batch.stop();
            } else {
                logger.info("batching channel {}", channel.getName());
                s3Batch.start();
                existingBatchGroups.remove(s3Batch.getGroupName());
            }
        }
        for (String groupName : existingBatchGroups) {
            logger.info("stopping unused group {}", groupName);
            webhookService.delete(groupName);
        }
    }

}

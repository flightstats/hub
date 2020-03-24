package com.flightstats.hub.dao.aws;

import com.flightstats.hub.app.HubServices;
import com.flightstats.hub.config.properties.AppProperties;
import com.flightstats.hub.config.properties.S3Properties;
import com.flightstats.hub.dao.Dao;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.replication.S3Batch;
import com.flightstats.hub.util.HubUtils;
import com.flightstats.hub.webhook.WebhookLeaderState;
import com.flightstats.hub.webhook.Webhook;
import com.flightstats.hub.webhook.WebhookService;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;

@Singleton
@Slf4j
public class S3BatchManager {

    private final WebhookService webhookService;
    private final Dao<ChannelConfig> channelConfigDao;
    private final HubUtils hubUtils;
    private final WebhookLeaderState webhookLeaderState;
    private final AppProperties appProperties;

    @Inject
    public S3BatchManager(WebhookService webhookService,
                          @Named("ChannelConfig") Dao<ChannelConfig> channelConfigDao,
                          HubUtils hubUtils,
                          WebhookLeaderState webhookLeaderState,
                          AppProperties appProperties,
                          S3Properties s3Properties) {
        this.webhookService = webhookService;
        this.channelConfigDao = channelConfigDao;
        this.hubUtils = hubUtils;
        this.webhookLeaderState = webhookLeaderState;
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

        for (ChannelConfig channel : channelConfigDao.getAll(false)) {
            S3Batch s3Batch = new S3Batch(
                    channel,
                    hubUtils,
                    appProperties.getAppUrl(),
                    appProperties.getAppEnv());

            if (channel.isSingle()) {
                WebhookLeaderState.RunningState state = webhookLeaderState.getState(channel.getName());
                if (!state.isStopped()) {
                    log.debug("turning off batch webhook for {}", channel.getDisplayName());
                    s3Batch.delete();
                }
            } else {
                log.info("batching channel {}", channel.getDisplayName());
                s3Batch.start();
                existingBatchGroups.remove(s3Batch.getGroupName());
            }
        }
        for (String groupName : existingBatchGroups) {
            log.info("deleting unused batch webhook {}", groupName);
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

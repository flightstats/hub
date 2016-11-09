package com.flightstats.hub.webhook;

import com.flightstats.hub.cluster.LastContentPath;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.dao.Dao;
import com.flightstats.hub.exception.ConflictException;
import com.flightstats.hub.exception.NoSuchChannelException;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.ContentPath;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

import static com.flightstats.hub.webhook.WebhookLeader.WEBHOOK_LAST_COMPLETED;

public class WebhookService {
    private final static Logger logger = LoggerFactory.getLogger(WebhookService.class);

    private final Dao<Webhook> webhookDao;
    private final WebhookValidator webhookValidator;
    private final WebhookManager webhookManager;
    private final LastContentPath lastContentPath;
    private ChannelService channelService;

    @Inject
    public WebhookService(@Named("Webhook") Dao<Webhook> webhookDao, WebhookValidator webhookValidator,
                          WebhookManager webhookManager, LastContentPath lastContentPath, ChannelService channelService) {
        this.webhookDao = webhookDao;
        this.webhookValidator = webhookValidator;
        this.webhookManager = webhookManager;
        this.lastContentPath = lastContentPath;
        this.channelService = channelService;
    }

    public Optional<Webhook> upsert(Webhook webhook) {
        logger.info("incoming webhook {} ", webhook);
        webhook = webhook.withDefaults();
        webhookValidator.validate(webhook);
        Optional<Webhook> webhookOptional = get(webhook.getName());
        if (webhookOptional.isPresent()) {
            Webhook existing = webhookOptional.get();
            if (existing.equals(webhook)) {
                return webhookOptional;
            } else if (!existing.allowedToChange(webhook)) {
                throw new ConflictException("{\"error\": \"channelUrl can not change. \"}");
            }
        } else {
            if (webhook.getStartingKey() == null) {
                webhook = webhook.withStartingKey(WebhookStrategy.createContentPath(webhook));
            }
        }
        logger.info("upsert webhook {} ", webhook);
        ContentPath existing = lastContentPath.getOrNull(webhook.getName(), WEBHOOK_LAST_COMPLETED);
        logger.info("webhook {} existing {} startingKey {}", webhook.getName(), existing, webhook.getStartingKey());
        if (existing == null || webhook.getStartingKey() != null) {
            logger.info("initializing {} with startingKey {}", webhook.getName(), webhook.getStartingKey());
            lastContentPath.initialize(webhook.getName(), webhook.getStartingKey(), WEBHOOK_LAST_COMPLETED);
        }
        webhookDao.upsert(webhook);
        webhookManager.notifyWatchers();
        return webhookOptional;
    }

    public Optional<Webhook> get(String name) {
        return Optional.fromNullable(webhookDao.get(name));
    }

    Optional<Webhook> getCached(String name) {
        return Optional.fromNullable(webhookDao.getCached(name));
    }

    public Collection<Webhook> getAll() {
        return webhookDao.getAll(false);
    }

    WebhookStatus getStatus(Webhook webhook) {
        WebhookStatus.WebhookStatusBuilder builder = WebhookStatus.builder().webhook(webhook);
        String channel = webhook.getChannelName();
        try {
            Optional<ContentKey> lastKey = channelService.getLatest(channel, true, false);
            if (lastKey.isPresent()) {
                builder.channelLatest(lastKey.get());
            }
        } catch (NoSuchChannelException e) {
            logger.info("no channel found for " + channel);
        }
        webhookManager.getStatus(webhook, builder);
        return builder.build();
    }

    public void delete(String name) {
        logger.info("deleting webhook " + name);
        webhookDao.delete(name);
        webhookManager.delete(name);
    }

}

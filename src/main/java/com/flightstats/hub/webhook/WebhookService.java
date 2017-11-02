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
import org.joda.time.DateTime;
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
        Optional<Webhook> preExisting = get(webhook.getName());
        if (webhook.isTagPrototype()) {
            return upsertTagWebhook(webhook, preExisting);
        }
        if (preExisting.isPresent()) {
            Webhook existing = preExisting.get();
            ContentPath newStartingKey = webhook.getStartingKey();
            if (newStartingKey != null && newStartingKey.getTime().compareTo(new DateTime(1980, 1, 1, 1, 1)) >= 0
                    && !newStartingKey.equals(existing.getStartingKey())) {
                updateCursor(webhook, webhook.getStartingKey());
            } else if (existing.equals(webhook)) {
                return preExisting;
            } else if (!existing.allowedToChange(webhook)) {
                throw new ConflictException("{\"error\": \"The channel name in the channelUrl can not change. \"}");
            }
        } else {
            if (webhook.getStartingKey() == null) {
                webhook = webhook.withStartingKey(WebhookStrategy.createContentPath(webhook));
            }
        }
        logger.info("upsert webhook {} ", webhook);
        ContentPath existingContentPath = lastContentPath.getOrNull(webhook.getName(), WEBHOOK_LAST_COMPLETED);
        logger.info("webhook {} existing {} startingKey {}", webhook.getName(), existingContentPath, webhook.getStartingKey());
        if (existingContentPath == null || webhook.getStartingKey() != null) {
            logger.info("initializing {} with startingKey {}", webhook.getName(), webhook.getStartingKey());
            lastContentPath.initialize(webhook.getName(), webhook.getStartingKey(), WEBHOOK_LAST_COMPLETED);
        }
        webhookDao.upsert(webhook);
        webhookManager.notifyWatchers(webhook);
        return preExisting;
    }

    private Optional<Webhook> upsertTagWebhook(Webhook webhook, Optional<Webhook> preExisting) {
        webhookDao.upsert(webhook);
        webhookManager.notifyWatchers(webhook);
        TagWebhook.upsertTagWebhookInstances(webhook);
        return preExisting;
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

    public Collection<Webhook> getAllCached() {
        return webhookDao.getAll(true);
    }

    WebhookStatus getStatus(Webhook webhook) {
        WebhookStatus.WebhookStatusBuilder builder = WebhookStatus.builder().webhook(webhook);
        if (webhook.isTagPrototype()) {
            return builder.build();
        }
        String channel = webhook.getChannelName();
        try {
            Optional<ContentKey> lastKey = channelService.getLatest(channel, true);
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
        TagWebhook.deleteInstancesIfTagWebhook(name);
        webhookDao.delete(name);
        webhookManager.delete(name);
    }

    void updateCursor(Webhook webhook, ContentPath item) {
        this.delete(webhook.getName());
        try // wait a few seconds? TODO - something more intelligent?
            {
                Thread.sleep(2000);
            } catch (InterruptedException ex) {
            logger.warn("Interruption exception while deleting");
                Thread.currentThread().interrupt();
            }
        this.upsert(webhook.withStartingKey(item));
    }
}

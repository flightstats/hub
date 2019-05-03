package com.flightstats.hub.webhook;

import com.flightstats.hub.cluster.LastContentPath;
import com.flightstats.hub.config.WebhookProperties;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.dao.Dao;
import com.flightstats.hub.dao.aws.ContentRetriever;
import com.flightstats.hub.exception.ConflictException;
import com.flightstats.hub.exception.NoSuchChannelException;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.ContentPath;
import com.flightstats.hub.util.RequestUtils;
import com.google.inject.name.Named;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;

import javax.inject.Inject;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.flightstats.hub.util.Constants.WEBHOOK_LAST_COMPLETED;

@Slf4j
public class WebhookService {

    private static final DateTime START_DATE_TIME =
            new DateTime(1980, 1, 1, 1, 1);

    private final Dao<Webhook> webhookDao;
    private final WebhookValidator webhookValidator;
    private final WebhookManager webhookManager;
    private final LastContentPath lastContentPath;
    private final ChannelService channelService;
    private final ContentRetriever contentRetriever;
    private final LocalWebhookManager localWebhookManager;
    private final WebhookProperties webhookProperties;

    @Inject
    public WebhookService(@Named("Webhook") Dao<Webhook> webhookDao,
                          WebhookValidator webhookValidator,
                          WebhookManager webhookManager,
                          LastContentPath lastContentPath,
                          ChannelService channelService,
                          ContentRetriever contentRetriever,
                          LocalWebhookManager localWebhookManager,
                          WebhookProperties webhookProperties) {
        this.webhookDao = webhookDao;
        this.webhookValidator = webhookValidator;
        this.webhookManager = webhookManager;
        this.lastContentPath = lastContentPath;
        this.channelService = channelService;
        this.contentRetriever = contentRetriever;
        this.localWebhookManager = localWebhookManager;
        this.webhookProperties = webhookProperties;
    }

    public Optional<Webhook> upsert(Webhook webhook) {
        log.info("incoming webhook {} ", webhook);
        webhook = webhook.withDefaults(webhookProperties.getCallbackTimeoutDefaultInSec());
        webhookValidator.validate(webhook);
        final Optional<Webhook> preExisting = get(webhook.getName());
        if (webhook.isTagPrototype()) {
            this.upsertTagWebhook(webhook);
            return preExisting;
        }
        if (preExisting.isPresent()) {
            final Webhook existing = preExisting.get();
            final ContentPath newStartingKey = webhook.getStartingKey();
            if (newStartingKey != null
                    && newStartingKey.getTime().compareTo(START_DATE_TIME) >= 0
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
        log.info("upsert webhook {} ", webhook);

        final ContentPath existingContentPath = lastContentPath.getOrNull(webhook.getName(), WEBHOOK_LAST_COMPLETED);
        log.info("webhook {} existing {} startingKey {}", webhook.getName(), existingContentPath, webhook.getStartingKey());
        if (existingContentPath == null || webhook.getStartingKey() != null) {
            log.info("initializing {} with startingKey {}", webhook.getName(), webhook.getStartingKey());
            lastContentPath.initialize(webhook.getName(), webhook.getStartingKey(), WEBHOOK_LAST_COMPLETED);
        }

        if (preExisting.isPresent()) {
            if (webhook.getErrorChannelUrl() != null && !webhook.getErrorChannelUrl().equals(preExisting.get().getErrorChannelUrl())) {
                createErrorChannel(webhook.getErrorChannelUrl());
            }
        } else {
            if (webhook.getErrorChannelUrl() != null) {
                createErrorChannel(webhook.getErrorChannelUrl());
            }
        }

        webhookDao.upsert(webhook);
        webhookManager.notifyWatchers(webhook);
        return preExisting;
    }

    private void upsertTagWebhook(Webhook webhook) {
        this.webhookDao.upsert(webhook);
        this.webhookManager.notifyWatchers(webhook);
        this.upsertTagWebhookInstances(webhook);
    }

    private void createErrorChannel(String channelURL) {
        String channelName = RequestUtils.getChannelName(channelURL);
        if (!this.contentRetriever.isExistingChannel(channelName)) {
            channelService.createChannel(ChannelConfig.builder().name(channelName).build());
        }
    }

    public Optional<Webhook> get(String name) {
        return Optional.ofNullable(webhookDao.get(name));
    }

    public Collection<Webhook> getAll() {
        return this.webhookDao.getAll(false);
    }

    public Collection<Webhook> getAllCached() {
        return this.webhookDao.getAll(true);
    }

    WebhookStatus getStatus(Webhook webhook) {
        final WebhookStatus.WebhookStatusBuilder builder = WebhookStatus.builder().webhook(webhook);
        if (webhook.isTagPrototype()) {
            return builder.build();
        }
        final String channel = webhook.getChannelName();
        try {
            Optional<ContentKey> lastKey = contentRetriever.getLatest(channel, true);
            lastKey.ifPresent(builder::channelLatest);
        } catch (NoSuchChannelException e) {
            log.info("no channel found for " + channel);
        }
        webhookManager.getStatus(webhook, builder);
        return builder.build();
    }

    public void delete(String name) {
        log.info("deleting webhook " + name);
        deleteInstancesIfTagWebhook(name);
        this.webhookDao.delete(name);
        this.webhookManager.delete(name);
    }

    void updateCursor(Webhook webhook, ContentPath item) {
        this.delete(webhook.getName());
        this.upsert(webhook.withStartingKey(item));
    }

    private void deleteInstancesIfTagWebhook(String webhookName) {
        final Optional<Webhook> webhookOptional = get(webhookName);
        if (!webhookOptional.isPresent()) return;
        final Webhook webhook = webhookOptional.get();
        if (!webhook.isTagPrototype()) return;
        log.info("TagWebHook: Deleting tag webhook instances for tag " + webhookName);

        final Set<String> names = webhookInstancesWithTag(webhook.getTagFromTagUrl()).stream()
                .map((Webhook::getName))
                .collect(Collectors.toSet());

        localWebhookManager.runAndWait("TagWebhook.deleteAll", names, this::delete);
    }

    // Add new wh instances for new or updated tag webhook
    private void upsertTagWebhookInstances(Webhook webhookPrototype) {
        final Collection<ChannelConfig> channels = channelService.getChannels(webhookPrototype.getTagFromTagUrl(), false);
        for (ChannelConfig channel : channels) {
            log.info("TagWebHook: Adding TagWebhook instance for " + channel.getName());
            upsert(Webhook.instanceFromTagPrototype(webhookPrototype, channel));
        }
    }

    Set<Webhook> webhookInstancesWithTag(String tag) {
        final Set<Webhook> webhookSet = new HashSet<>(this.webhookDao.getAll(false));

        return webhookSet.stream()
                .filter(wh -> !wh.isTagPrototype() && Objects.equals(tag, wh.getManagedByTag()))
                .collect(Collectors.toSet());
    }

}

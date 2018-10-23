package com.flightstats.hub.webhook;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.cluster.LastContentPath;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.dao.Dao;
import com.flightstats.hub.exception.ConflictException;
import com.flightstats.hub.exception.InvalidRequestException;
import com.flightstats.hub.exception.NoSuchChannelException;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.ContentPath;
import com.flightstats.hub.model.DirectionQuery;
import com.flightstats.hub.util.RequestUtils;
import com.google.common.base.Optional;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.util.Collection;
import java.util.SortedSet;

import static com.flightstats.hub.webhook.WebhookLeader.WEBHOOK_LAST_COMPLETED;

public class WebhookService {

    private final static Logger logger = LoggerFactory.getLogger(WebhookService.class);

    private final Dao<Webhook> webhookDao;
    private final WebhookValidator webhookValidator;
    private final WebhookManager webhookManager;
    private final LastContentPath lastContentPath;
    private final ChannelService channelService;
    private final ObjectMapper mapper;
    private final HubProperties hubProperties;

    @Inject
    public WebhookService(@Named("Webhook") Dao<Webhook> webhookDao,
                          WebhookValidator webhookValidator,
                          WebhookManager webhookManager,
                          LastContentPath lastContentPath,
                          ChannelService channelService,
                          ObjectMapper mapper,
                          HubProperties hubProperties)
    {
        this.webhookDao = webhookDao;
        this.webhookValidator = webhookValidator;
        this.webhookManager = webhookManager;
        this.lastContentPath = lastContentPath;
        this.channelService = channelService;
        this.mapper = mapper;
        this.hubProperties = hubProperties;
    }

    public Optional<Webhook> upsert(Webhook webhook) {
        logger.info("incoming webhook {} ", webhook);
        webhook = withDefaults(webhook);
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

    private void createErrorChannel(String channelURL) {
        String channelName = RequestUtils.getChannelName(channelURL);
        if (!channelService.channelExists(channelName)) {
            channelService.createChannel(ChannelConfig.builder().name(channelName).build());
        }
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

    public Webhook fromJson(String json) {
        return fromJson(json, Optional.absent());
    }

    public Webhook fromJson(String json, Optional<Webhook> webhookOptional) {
        Webhook.WebhookBuilder builder;
        if (webhookOptional.isPresent()) {
            builder = webhookOptional.get().toBuilder();
        } else {
            builder = Webhook.builder();
        }

        try {
            JsonNode root = mapper.readTree(json);
            if (root.has("startItem")) {
                Optional<ContentPath> keyOptional = Optional.absent();
                String startItem = root.get("startItem").asText();
                if (startItem.equalsIgnoreCase("previous")) {
                    keyOptional = getPrevious(keyOptional, root.get("channelUrl").asText());
                } else {
                    keyOptional = ContentPath.fromFullUrl(startItem);
                }
                if (keyOptional.isPresent()) {
                    builder.startingKey(keyOptional.get());
                }
            } else if (root.has("lastCompleted")) {
                Optional<ContentPath> keyOptional = ContentPath.fromFullUrl(root.get("lastCompleted").asText());
                if (keyOptional.isPresent()) {
                    builder.startingKey(keyOptional.get());
                }
            }
            if (root.has("name")) {
                builder.name(root.get("name").asText());
            }
            if (root.has("paused")) {
                builder.paused(root.get("paused").asBoolean());
            }
            if (root.has("callbackUrl")) {
                builder.callbackUrl(root.get("callbackUrl").asText());
            }
            if (root.has("channelUrl")) {
                builder.channelUrl(root.get("channelUrl").asText());
            }
            if (root.has("parallelCalls")) {
                builder.parallelCalls(root.get("parallelCalls").intValue());
            }
            if (root.has("batch")) {
                builder.batch(root.get("batch").asText());
            }
            if (root.has("heartbeat")) {
                builder.heartbeat(root.get("heartbeat").asBoolean());
            }
            if (root.has("ttlMinutes")) {
                builder.ttlMinutes(root.get("ttlMinutes").intValue());
            }
            if (root.has("maxWaitMinutes")) {
                builder.maxWaitMinutes(root.get("maxWaitMinutes").intValue());
            }
            if (root.has("callbackTimeoutSeconds")) {
                builder.callbackTimeoutSeconds(root.get("callbackTimeoutSeconds").intValue());
            }
            if (root.has("fastForwardable")) {
                builder.fastForwardable(root.get("fastForwardable").asBoolean());
            }
            if (root.has("tagUrl")) {
                String t = root.get("tagUrl").asText().isEmpty() ? null : root.get("tagUrl").asText();
                builder.tagUrl(t);
            }
            if (root.has("tag")) {
                builder.managedByTag(root.get("tag").asText());
            }
            if (root.has("maxAttempts")) {
                builder.maxAttempts(root.get("maxAttempts").intValue());
            }
            if (root.has("errorChannelUrl")) {
                builder.errorChannelUrl(root.get("errorChannelUrl").asText());
            }
        } catch (IOException e) {
            logger.warn("unable to parse json" + json, e);
            throw new InvalidRequestException(e.getMessage());
        }

        return builder.build();
    }

    private Optional<ContentPath> getPrevious(Optional<ContentPath> keyOptional, String channelUrl) {
        String channel = RequestUtils.getChannelName(channelUrl);
        Optional<ContentKey> latest = channelService.getLatest(channel, true);
        if (latest.isPresent()) {
            DirectionQuery query = DirectionQuery.builder()
                    .channelName(channel)
                    .startKey(latest.get())
                    .next(false)
                    .count(1)
                    .build();
            SortedSet<ContentKey> keys = channelService.query(query);
            if (keys.isEmpty()) {
                keyOptional = Optional.of(new ContentKey(latest.get().getTime().minusMillis(1), "A"));
            } else {
                keyOptional = Optional.of(keys.first());
            }
        }
        return keyOptional;
    }

    Webhook fromTagPrototype(Webhook prototype, String channelName) {
        String channelUrl = String.format("%s/channel/%s", RequestUtils.getHost(prototype.getTagUrl()), channelName);
        String name = String.format("TAGWH_%s_%s", prototype.getTagFromTagUrl(), channelName);
        return prototype.toBuilder()
                .name(name)
                .channelUrl(channelUrl)
                .startingKey(null)
                .tagUrl(null)
                .managedByTag(prototype.getTagFromTagUrl())
                .build();
    }

    /**
     * Returns a Webhook with all optional values set to the default.
     */
    public Webhook withDefaults(Webhook original) {
        Webhook.WebhookBuilder builder = original.toBuilder();

        if (original.getParallelCalls() == null) {
            builder.parallelCalls(1);
        }

        if (original.getBatch() == null) {
            builder.batch(Webhook.SINGLE);
        }

        if (original.isMinute() || original.isSecond()) {
            builder.heartbeat(true);
        }

        if (original.getTtlMinutes() == null) {
            builder.ttlMinutes(0);
        }

        if (original.getMaxWaitMinutes() == null) {
            builder.maxWaitMinutes(1);
        }

        if (original.getCallbackTimeoutSeconds() == null) {
            builder.callbackTimeoutSeconds(hubProperties.getCallbackTimeoutDefault());
        }

        if (original.getMaxAttempts() == null) {
            builder.maxAttempts(0);
        }

        return builder.build();
    }

}

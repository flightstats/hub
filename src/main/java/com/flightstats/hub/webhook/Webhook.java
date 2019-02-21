package com.flightstats.hub.webhook;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.app.HubProvider;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.exception.InvalidRequestException;
import com.flightstats.hub.model.*;
import com.flightstats.hub.util.RequestUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.danielbechler.diff.ObjectDifferBuilder;
import de.danielbechler.diff.node.DiffNode;
import lombok.*;
import lombok.experimental.Wither;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedSet;

@Builder
@Getter
@ToString
@EqualsAndHashCode
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Webhook implements Comparable<Webhook>, NamedType {
    public static final String SINGLE = "SINGLE";
    public static final String MINUTE = "MINUTE";
    public static final String SECOND = "SECOND";
    private final static Logger logger = LoggerFactory.getLogger(Webhook.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final Gson gson = new GsonBuilder().create();
    private final String callbackUrl;
    private final String channelUrl;
    @Wither
    private final Integer parallelCalls;
    @Wither
    private final String name;
    @Wither
    private final transient ContentPath startingKey;
    @Wither
    private final String batch;
    @Wither
    private final boolean heartbeat;
    @Wither
    private final boolean paused;
    @Wither
    private final Integer ttlMinutes;
    @Wither
    private final Integer maxWaitMinutes;
    @Wither
    private final Integer callbackTimeoutSeconds;
    private final boolean fastForwardable;
    @Wither
    private final String tagUrl;  // webhooks with tagUrl defined are prototype webhook definitions
    @Wither
    private final String managedByTag;  // webhooks with tag set were created by a tag webhook prototype and will be automatically
    // created and deleted when a channel has the webhook "tagUrl" added or removed.
    @Wither
    private final Integer maxAttempts;
    @Wither
    private final String errorChannelUrl;

    boolean secondaryMetricsReporting;

    static Webhook fromJson(String json, Optional<Webhook> webhookOptional) {
        WebhookBuilder builder = Webhook.builder();
        if (webhookOptional.isPresent()) {
            Webhook existing = webhookOptional.get();
            builder.parallelCalls(existing.parallelCalls)
                    .paused(existing.paused)
                    .callbackUrl(existing.callbackUrl)
                    .channelUrl(existing.channelUrl)
                    .name(existing.name)
                    .startingKey(existing.startingKey)
                    .batch(existing.batch)
                    .ttlMinutes(existing.ttlMinutes)
                    .maxWaitMinutes(existing.maxWaitMinutes)
                    .callbackTimeoutSeconds(existing.callbackTimeoutSeconds)
                    .heartbeat(existing.heartbeat)
                    .fastForwardable(existing.fastForwardable)
                    .tagUrl(existing.tagUrl)
                    .managedByTag(existing.managedByTag)
                    .maxAttempts(existing.maxAttempts)
                    .errorChannelUrl(existing.errorChannelUrl)
                    .secondaryMetricsReporting(existing.secondaryMetricsReporting);
        }
        try {
            JsonNode root = mapper.readTree(json);
            if (root.has("startItem")) {
                Optional<ContentPath> keyOptional = Optional.empty();
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
            if (root.has("secondaryMetricsReporting")) {
                builder.secondaryMetricsReporting(root.get("secondaryMetricsReporting").asBoolean());
            }
        } catch (IOException e) {
            logger.warn("unable to parse json" + json, e);
            throw new InvalidRequestException(e.getMessage());
        }
        return builder.build();
    }

    @JsonIgnore
    boolean isTagPrototype() {
        return !StringUtils.isEmpty(this.tagUrl);
    }

    String getTagFromTagUrl() {
        return RequestUtils.getTag(this.getTagUrl());
    }

    boolean isManagedByTag() {
        return !StringUtils.isEmpty(managedByTag);
    }

    private static Optional<ContentPath> getPrevious(Optional<ContentPath> keyOptional, String channelUrl) {
        ChannelService channelService = HubProvider.getInstance(ChannelService.class);
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

    static Webhook instanceFromTagPrototype(Webhook whp, ChannelConfig channel) {
        String channelUrl = RequestUtils.getHost(whp.getTagUrl()) + "/channel/" + channel.getName();
        String whName = "TAGWH_" + whp.getTagFromTagUrl() + "_" + channel.getName();
        return new Webhook(whp.callbackUrl, channelUrl, whp.parallelCalls, whName, null, whp.batch, whp.heartbeat, whp.paused, whp.ttlMinutes, whp.maxWaitMinutes, whp.callbackTimeoutSeconds, whp.fastForwardable, null, whp.getTagFromTagUrl(), whp.maxAttempts, whp.errorChannelUrl, whp.secondaryMetricsReporting);
    }

    public static Webhook fromJson(String json) {
        return fromJson(json, Optional.empty());
    }

    @JsonIgnore
    ContentPath getStartingKey() {
        return startingKey;
    }

    boolean allowedToChange(Webhook other) {
        return getChannelName().equals(other.getChannelName())
                && name.equals(other.name);
    }

    boolean isChanged(Webhook other) {
        DiffNode diff = ObjectDifferBuilder.buildDefault().compare(this, other);
        return !Objects.equals(parallelCalls, other.parallelCalls) || diff.hasChanges();
    }

    public String toJson() {
        return gson.toJson(this);
    }

    /**
     * Returns a Webhook with all optional values set to the default.
     */
    public Webhook withDefaults() {
        Webhook webhook = this;
        if (parallelCalls == null) {
            webhook = webhook.withParallelCalls(1);
        }
        if (batch == null) {
            webhook = webhook.withBatch("SINGLE");
        }
        if (webhook.isMinute() || webhook.isSecond()) {
            webhook = webhook.withHeartbeat(true);
        }
        if (ttlMinutes == null) {
            webhook = webhook.withTtlMinutes(0);
        }
        if (maxWaitMinutes == null) {
            webhook = webhook.withMaxWaitMinutes(1);
        }
        if (callbackTimeoutSeconds == null) {
            webhook = webhook.withCallbackTimeoutSeconds(HubProperties.getCallbackTimeoutDefault());
        }
        if (maxAttempts == null) {
            webhook = webhook.withMaxAttempts(0);
        }
        return webhook;
    }

    public boolean isMinute() {
        return MINUTE.equalsIgnoreCase(getBatch());
    }

    public boolean isSecond() {
        return SECOND.equalsIgnoreCase(getBatch());
    }

    public Integer getTtlMinutes() {
        if (ttlMinutes == null) {
            return 0;
        }
        return ttlMinutes;
    }

    @Override
    public int compareTo(Webhook other) {
        return getName().compareTo(other.getName());
    }

    public String getChannelName() {
        return RequestUtils.getChannelName(getChannelUrl());
    }

}

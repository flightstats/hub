package com.flightstats.hub.webhook;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flightstats.hub.dao.aws.ContentRetriever;
import com.flightstats.hub.exception.InvalidRequestException;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.ContentPath;
import com.flightstats.hub.model.DirectionQuery;
import com.flightstats.hub.model.NamedType;
import com.flightstats.hub.util.RequestUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.danielbechler.diff.ObjectDifferBuilder;
import de.danielbechler.diff.node.DiffNode;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Wither;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedSet;

import static com.flightstats.hub.model.WebhookType.MINUTE;
import static com.flightstats.hub.model.WebhookType.SECOND;

@Builder
@Getter
@ToString
@EqualsAndHashCode
@Slf4j
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Webhook implements Comparable<Webhook>, NamedType {

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

    static Webhook fromJson(String json, Optional<Webhook> webhookOptional, ContentRetriever contentRetriever) {
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
                    keyOptional = getPrevious(keyOptional, root.get("channelUrl").asText(), contentRetriever);
                } else {
                    keyOptional = ContentPath.fromFullUrl(startItem);
                }
                keyOptional.ifPresent(builder::startingKey);
            } else if (root.has("lastCompleted")) {
                final Optional<ContentPath> keyOptional = ContentPath.fromFullUrl(root.get("lastCompleted").asText());
                keyOptional.ifPresent(builder::startingKey);
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
            log.warn("unable to parse json" + json, e);
            throw new InvalidRequestException(e.getMessage());
        }
        return builder.build();
    }

    private static Optional<ContentPath> getPrevious(Optional<ContentPath> keyOptional, String channelUrl, ContentRetriever contentRetriever) {
        final String channel = RequestUtils.getChannelName(channelUrl);
        final Optional<ContentKey> latest = contentRetriever.getLatest(channel, true);
        if (latest.isPresent()) {
            DirectionQuery query = DirectionQuery.builder()
                    .channelName(channel)
                    .startKey(latest.get())
                    .next(false)
                    .count(1)
                    .build();
            SortedSet<ContentKey> keys = contentRetriever.query(query);
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

    public static Webhook fromJson(String json, ContentRetriever contentRetriever) {
        return fromJson(json, Optional.empty(), contentRetriever);
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

    @JsonIgnore
    public ContentPath getStartingKey() {
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
    public Webhook withDefaults(Integer callbackTimeoutSeconds) {
        Webhook webhook = this;
        if (this.parallelCalls == null) {
            webhook = webhook.withParallelCalls(1);
        }
        if (this.batch == null) {
            webhook = webhook.withBatch("SINGLE");
        }
        if (webhook.isMinute() || webhook.isSecond()) {
            webhook = webhook.withHeartbeat(true);
        }
        if (this.ttlMinutes == null) {
            webhook = webhook.withTtlMinutes(0);
        }
        if (this.maxWaitMinutes == null) {
            webhook = webhook.withMaxWaitMinutes(1);
        }
        if (this.callbackTimeoutSeconds == null) {
            webhook = webhook.withCallbackTimeoutSeconds(callbackTimeoutSeconds);
        }
        if (this.maxAttempts == null) {
            webhook = webhook.withMaxAttempts(0);
        }
        return webhook;
    }

    public boolean isMinute() {
        return MINUTE.name().equalsIgnoreCase(getBatch());
    }

    public boolean isSecond() {
        return SECOND.name().equalsIgnoreCase(getBatch());
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

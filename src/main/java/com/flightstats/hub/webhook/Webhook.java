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
import com.google.common.base.Optional;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.danielbechler.diff.ObjectDifferBuilder;
import de.danielbechler.diff.node.DiffNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Objects;
import java.util.SortedSet;

public class Webhook implements Comparable<Webhook>, NamedType {
    public static final String SINGLE = "SINGLE";
    public static final String MINUTE = "MINUTE";
    public static final String SECOND = "SECOND";
    private final static Logger logger = LoggerFactory.getLogger(Webhook.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final Gson gson = new GsonBuilder().create();
    private final String callbackUrl;
    private final String channelUrl;
    private final Integer parallelCalls;
    private final String name;
    private final transient ContentPath startingKey;
    private final String batch;
    private final boolean heartbeat;
    private final boolean paused;
    private final Integer ttlMinutes;
    private final Integer maxWaitMinutes;
    private final Integer callbackTimeoutSeconds;
    private final boolean fastForwardable;
    private final String tag;  // webhooks with tag defined are prototype webhook definitions
    private final boolean managedByTag; // webhooks with this bit set were created by a tag prototype and will be automatically
    // created and deleted when a channel has the webhook "tag" added or removed.

    @java.beans.ConstructorProperties({"callbackUrl", "channelUrl", "parallelCalls", "name", "startingKey", "batch", "heartbeat", "paused", "ttlMinutes", "maxWaitMinutes", "callbackTimeoutSeconds"})
    private Webhook(String callbackUrl, String channelUrl, Integer parallelCalls, String name, ContentPath startingKey, String batch, boolean heartbeat, boolean paused, Integer ttlMinutes, Integer maxWaitMinutes, Integer callbackTimeoutSeconds, boolean fastForwardable, String tag, boolean managedByTag) {
        this.callbackUrl = callbackUrl;
        this.channelUrl = channelUrl;
        this.parallelCalls = parallelCalls;
        this.name = name;
        this.startingKey = startingKey;
        this.batch = batch;
        this.heartbeat = heartbeat;
        this.paused = paused;
        this.ttlMinutes = ttlMinutes;
        this.maxWaitMinutes = maxWaitMinutes;
        this.callbackTimeoutSeconds = callbackTimeoutSeconds;
        this.fastForwardable = fastForwardable;
        this.tag = tag;
        this.managedByTag = managedByTag;
    }

    public static Webhook fromJson(String json, Optional<Webhook> webhookOptional) {
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
                    .tag(existing.tag)
                    .managedByTag(existing.managedByTag);
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
            } else if (root.has("lastCompletedCallback")) {
                Optional<ContentPath> keyOptional = ContentPath.fromFullUrl(root.get("lastCompletedCallback").asText());
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
            if (root.has("tag")) {
                builder.tag(root.get("tag").asText());
            }
            if (root.has("managedByTag")) {
                builder.managedByTag(root.get("managedByTag").asBoolean());
            }
        } catch (IOException e) {
            logger.warn("unable to parse json" + json, e);
            throw new InvalidRequestException(e.getMessage());
        }
        return builder.build();
    }

    @JsonIgnore
    public boolean isTagPrototype() {
        return tag != null && !tag.isEmpty() && !isManagedByTag();
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

    public static Webhook instanceFromTagPrototype(Webhook whp, ChannelConfig channel) {
        String channenUrl = whp.getChannelUrl() + "/channel/" + channel.getName();
        String whName = "TAGWH_" + whp.getTag() + "_" + channel.getName();
        Webhook instance = new Webhook(whp.callbackUrl, channenUrl, whp.parallelCalls, whName, null, whp.batch, whp.heartbeat, whp.paused, whp.ttlMinutes, whp.maxWaitMinutes, whp.callbackTimeoutSeconds, whp.fastForwardable, whp.tag, true);
        return instance;
    }

    public static Webhook fromJson(String json) {
        return fromJson(json, Optional.absent());
    }

    public static WebhookBuilder builder() {
        return new WebhookBuilder();
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

    public String getCallbackUrl() {
        return this.callbackUrl;
    }

    public String getChannelUrl() {
        return this.channelUrl;
    }

    public Integer getParallelCalls() {
        return this.parallelCalls;
    }

    public String getName() {
        return this.name;
    }

    public String getTag() {
        return this.tag;
    }

    public String getBatch() {
        return this.batch;
    }

    public boolean isHeartbeat() {
        return this.heartbeat;
    }

    public boolean isPaused() {
        return this.paused;
    }

    public Integer getMaxWaitMinutes() {
        return this.maxWaitMinutes;
    }

    public Integer getCallbackTimeoutSeconds() {
        return this.callbackTimeoutSeconds;
    }

    public boolean isFastForwardable() {
        return this.fastForwardable;
    }

    public boolean isManagedByTag() {
        return this.managedByTag;
    }

    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof Webhook)) return false;
        final Webhook other = (Webhook) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$callbackUrl = this.getCallbackUrl();
        final Object other$callbackUrl = other.getCallbackUrl();
        if (this$callbackUrl == null ? other$callbackUrl != null : !this$callbackUrl.equals(other$callbackUrl))
            return false;
        final Object this$channelUrl = this.getChannelUrl();
        final Object other$channelUrl = other.getChannelUrl();
        if (this$channelUrl == null ? other$channelUrl != null : !this$channelUrl.equals(other$channelUrl))
            return false;
        final Object this$parallelCalls = this.getParallelCalls();
        final Object other$parallelCalls = other.getParallelCalls();
        if (this$parallelCalls == null ? other$parallelCalls != null : !this$parallelCalls.equals(other$parallelCalls))
            return false;
        final Object this$name = this.getName();
        final Object other$name = other.getName();
        if (this$name == null ? other$name != null : !this$name.equals(other$name)) return false;
        final Object this$tag = this.getTag();
        final Object other$tag = other.getTag();
        if (this$tag == null ? other$tag != null : !this$tag.equals(other$tag)) return false;
        final Object this$batch = this.getBatch();
        final Object other$batch = other.getBatch();
        if (this$batch == null ? other$batch != null : !this$batch.equals(other$batch)) return false;
        if (this.isHeartbeat() != other.isHeartbeat()) return false;
        if (this.isPaused() != other.isPaused()) return false;
        final Object this$ttlMinutes = this.getTtlMinutes();
        final Object other$ttlMinutes = other.getTtlMinutes();
        if (this$ttlMinutes == null ? other$ttlMinutes != null : !this$ttlMinutes.equals(other$ttlMinutes))
            return false;
        final Object this$maxWaitMinutes = this.getMaxWaitMinutes();
        final Object other$maxWaitMinutes = other.getMaxWaitMinutes();
        if (this$maxWaitMinutes == null ? other$maxWaitMinutes != null : !this$maxWaitMinutes.equals(other$maxWaitMinutes))
            return false;
        final Object this$callbackTimeoutSeconds = this.getCallbackTimeoutSeconds();
        final Object other$callbackTimeoutSeconds = other.getCallbackTimeoutSeconds();
        if (this$callbackTimeoutSeconds == null ? other$callbackTimeoutSeconds != null : !this$callbackTimeoutSeconds.equals(other$callbackTimeoutSeconds))
            return false;
        final Object this$managedByTag = this.isManagedByTag();
        final Object other$managedByTag = other.isManagedByTag();
        if (this$managedByTag == null ? other$managedByTag != null : !this$managedByTag.equals(other$managedByTag))
            return false;
        return true;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $callbackUrl = this.getCallbackUrl();
        result = result * PRIME + ($callbackUrl == null ? 43 : $callbackUrl.hashCode());
        final Object $channelUrl = this.getChannelUrl();
        result = result * PRIME + ($channelUrl == null ? 43 : $channelUrl.hashCode());
        final Object $parallelCalls = this.getParallelCalls();
        result = result * PRIME + ($parallelCalls == null ? 43 : $parallelCalls.hashCode());
        final Object $name = this.getName();
        result = result * PRIME + ($name == null ? 43 : $name.hashCode());
        final Object $batch = this.getBatch();
        result = result * PRIME + ($batch == null ? 43 : $batch.hashCode());
        result = result * PRIME + (this.isHeartbeat() ? 79 : 97);
        result = result * PRIME + (this.isPaused() ? 79 : 97);
        final Object $ttlMinutes = this.getTtlMinutes();
        result = result * PRIME + ($ttlMinutes == null ? 43 : $ttlMinutes.hashCode());
        final Object $maxWaitMinutes = this.getMaxWaitMinutes();
        result = result * PRIME + ($maxWaitMinutes == null ? 43 : $maxWaitMinutes.hashCode());
        final Object $callbackTimeoutSeconds = this.getCallbackTimeoutSeconds();
        result = result * PRIME + ($callbackTimeoutSeconds == null ? 43 : $callbackTimeoutSeconds.hashCode());
        return result;
    }

    protected boolean canEqual(Object other) {
        return other instanceof Webhook;
    }

    public String toString() {
        return "com.flightstats.hub.webhook.Webhook(callbackUrl=" + this.getCallbackUrl()
                + ", channelUrl=" + this.getChannelUrl()
                + ", parallelCalls=" + this.getParallelCalls()
                + ", name=" + this.getName()
                + ", startingKey=" + this.getStartingKey()
                + ", batch=" + this.getBatch()
                + ", heartbeat=" + this.isHeartbeat()
                + ", paused=" + this.isPaused()
                + ", ttlMinutes=" + this.getTtlMinutes()
                + ", maxWaitMinutes=" + this.getMaxWaitMinutes()
                + ", callbackTimeoutSeconds=" + this.getCallbackTimeoutSeconds() + ")";
    }

    public Webhook withParallelCalls(Integer parallelCalls) {
        return this.parallelCalls == parallelCalls ? this : new Webhook(this.callbackUrl, this.channelUrl, parallelCalls, this.name, this.startingKey, this.batch, this.heartbeat, this.paused, this.ttlMinutes, this.maxWaitMinutes, this.callbackTimeoutSeconds, this.fastForwardable, this.tag, this.managedByTag);
    }

    public Webhook withName(String name) {
        return this.name == name ? this : new Webhook(this.callbackUrl, this.channelUrl, this.parallelCalls, name, this.startingKey, this.batch, this.heartbeat, this.paused, this.ttlMinutes, this.maxWaitMinutes, this.callbackTimeoutSeconds, this.fastForwardable, this.tag, this.managedByTag);
    }

    public Webhook withStartingKey(ContentPath startingKey) {
        return this.startingKey == startingKey ? this : new Webhook(this.callbackUrl, this.channelUrl, this.parallelCalls, this.name, startingKey, this.batch, this.heartbeat, this.paused, this.ttlMinutes, this.maxWaitMinutes, this.callbackTimeoutSeconds, this.fastForwardable, this.tag, this.managedByTag);
    }

    public Webhook withBatch(String batch) {
        return this.batch == batch ? this : new Webhook(this.callbackUrl, this.channelUrl, this.parallelCalls, this.name, this.startingKey, batch, this.heartbeat, this.paused, this.ttlMinutes, this.maxWaitMinutes, this.callbackTimeoutSeconds, this.fastForwardable, this.tag, this.managedByTag);
    }

    public Webhook withHeartbeat(boolean heartbeat) {
        return this.heartbeat == heartbeat ? this : new Webhook(this.callbackUrl, this.channelUrl, this.parallelCalls, this.name, this.startingKey, this.batch, heartbeat, this.paused, this.ttlMinutes, this.maxWaitMinutes, this.callbackTimeoutSeconds, this.fastForwardable, this.tag, this.managedByTag);
    }

    public Webhook withPaused(boolean paused) {
        return this.paused == paused ? this : new Webhook(this.callbackUrl, this.channelUrl, this.parallelCalls, this.name, this.startingKey, this.batch, this.heartbeat, paused, this.ttlMinutes, this.maxWaitMinutes, this.callbackTimeoutSeconds, this.fastForwardable, this.tag, this.managedByTag);
    }

    public Webhook withTtlMinutes(Integer ttlMinutes) {
        return this.ttlMinutes == ttlMinutes ? this : new Webhook(this.callbackUrl, this.channelUrl, this.parallelCalls, this.name, this.startingKey, this.batch, this.heartbeat, this.paused, ttlMinutes, this.maxWaitMinutes, this.callbackTimeoutSeconds, this.fastForwardable, this.tag, this.managedByTag);
    }

    public Webhook withMaxWaitMinutes(Integer maxWaitMinutes) {
        return this.maxWaitMinutes == maxWaitMinutes ? this : new Webhook(this.callbackUrl, this.channelUrl, this.parallelCalls, this.name, this.startingKey, this.batch, this.heartbeat, this.paused, this.ttlMinutes, maxWaitMinutes, this.callbackTimeoutSeconds, this.fastForwardable, this.tag, this.managedByTag);
    }

    public Webhook withCallbackTimeoutSeconds(Integer callbackTimeoutSeconds) {
        return this.callbackTimeoutSeconds == callbackTimeoutSeconds ? this : new Webhook(this.callbackUrl, this.channelUrl, this.parallelCalls, this.name, this.startingKey, this.batch, this.heartbeat, this.paused, this.ttlMinutes, this.maxWaitMinutes, callbackTimeoutSeconds, this.fastForwardable, this.tag, this.managedByTag);
    }

    public Webhook withFastForwardable(boolean fastForwardable) {
        return this.fastForwardable == fastForwardable ? this : new Webhook(this.callbackUrl, this.channelUrl, this.parallelCalls, this.name, this.startingKey, this.batch, this.heartbeat, this.paused, this.ttlMinutes, this.maxWaitMinutes, callbackTimeoutSeconds, this.fastForwardable, this.tag, this.managedByTag);
    }

    public static class WebhookBuilder {
        private String callbackUrl;
        private String channelUrl;
        private Integer parallelCalls;
        private String name;
        private ContentPath startingKey;
        private String batch;
        private boolean heartbeat;
        private boolean paused;
        private Integer ttlMinutes;
        private Integer maxWaitMinutes;
        private Integer callbackTimeoutSeconds;
        private boolean fastForwardable;
        private String tag;
        private boolean managedByTag;
        private boolean isTagPrototype;

        WebhookBuilder() {
        }

        public Webhook.WebhookBuilder callbackUrl(String callbackUrl) {
            this.callbackUrl = callbackUrl;
            return this;
        }

        public Webhook.WebhookBuilder channelUrl(String channelUrl) {
            this.channelUrl = channelUrl;
            return this;
        }

        public Webhook.WebhookBuilder parallelCalls(Integer parallelCalls) {
            this.parallelCalls = parallelCalls;
            return this;
        }

        public Webhook.WebhookBuilder name(String name) {
            this.name = name;
            return this;
        }

        public Webhook.WebhookBuilder tag(String tag) {
            this.tag = tag;
            return this;
        }

        public Webhook.WebhookBuilder startingKey(ContentPath startingKey) {
            this.startingKey = startingKey;
            return this;
        }

        public Webhook.WebhookBuilder batch(String batch) {
            this.batch = batch;
            return this;
        }

        public Webhook.WebhookBuilder heartbeat(boolean heartbeat) {
            this.heartbeat = heartbeat;
            return this;
        }

        public Webhook.WebhookBuilder paused(boolean paused) {
            this.paused = paused;
            return this;
        }

        public Webhook.WebhookBuilder ttlMinutes(Integer ttlMinutes) {
            this.ttlMinutes = ttlMinutes;
            return this;
        }

        public Webhook.WebhookBuilder maxWaitMinutes(Integer maxWaitMinutes) {
            this.maxWaitMinutes = maxWaitMinutes;
            return this;
        }

        public Webhook.WebhookBuilder callbackTimeoutSeconds(Integer callbackTimeoutSeconds) {
            this.callbackTimeoutSeconds = callbackTimeoutSeconds;
            return this;
        }

        public Webhook.WebhookBuilder fastForwardable(boolean fastForwardable) {
            this.fastForwardable = fastForwardable;
            return this;
        }

        public Webhook.WebhookBuilder managedByTag(boolean managedByTag) {
            this.managedByTag = managedByTag;
            return this;
        }

        public Webhook.WebhookBuilder isTagPrototype(boolean isTagPrototype) {
            this.isTagPrototype = isTagPrototype;
            return this;
        }

        public Webhook build() {
            return new Webhook(callbackUrl, channelUrl, parallelCalls, name, startingKey, batch, heartbeat, paused, ttlMinutes, maxWaitMinutes, callbackTimeoutSeconds, fastForwardable, tag, managedByTag);
        }

        public String toString() {
            return "com.flightstats.hub.webhook.Webhook.WebhookBuilder(callbackUrl=" + this.callbackUrl
                    + ", channelUrl=" + this.channelUrl
                    + ", parallelCalls=" + this.parallelCalls
                    + ", name=" + this.name
                    + ", tag=" + this.tag
                    + ", managedByTag=" + this.managedByTag
                    + ", startingKey=" + this.startingKey
                    + ", batch=" + this.batch
                    + ", heartbeat=" + this.heartbeat
                    + ", paused=" + this.paused
                    + ", ttlMinutes=" + this.ttlMinutes
                    + ", maxWaitMinutes=" + this.maxWaitMinutes
                    + ", callbackTimeoutSeconds=" + this.callbackTimeoutSeconds + ")";
        }
    }
}

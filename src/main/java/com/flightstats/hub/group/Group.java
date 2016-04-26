package com.flightstats.hub.group;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flightstats.hub.app.HubProvider;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.ContentPath;
import com.flightstats.hub.model.DirectionQuery;
import com.flightstats.hub.util.ChannelNameUtils;
import com.google.common.base.Optional;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.*;
import lombok.experimental.Wither;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.SortedSet;

@Builder
@Getter
@ToString
@EqualsAndHashCode(exclude = {"startingKey"})
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Group {
    private final static Logger logger = LoggerFactory.getLogger(Group.class);

    public static final String SINGLE = "SINGLE";
    public static final String MINUTE = "MINUTE";
    public static final String SECOND = "SECOND";

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
    private final boolean paused;
    @Wither
    private final Integer ttlMinutes;
    @Wither
    private final Integer maxWaitMinutes;

    private static final ObjectMapper mapper = new ObjectMapper();

    @JsonIgnore
    public ContentPath getStartingKey() {
        return startingKey;
    }

    public boolean allowedToChange(Group other) {
        return channelUrl.equals(other.channelUrl)
                && name.equals(other.name);
    }

    public boolean isChanged(Group other) {
        return parallelCalls != other.parallelCalls
                || paused != other.paused
                || !callbackUrl.equals(other.callbackUrl)
                || !batch.equals(other.batch)
                || !heartbeat == other.heartbeat
                || !ttlMinutes.equals(other.ttlMinutes)
                || !maxWaitMinutes.equals(other.maxWaitMinutes);
    }

    private static final Gson gson = new GsonBuilder().create();

    public String toJson() {
        return gson.toJson(this);
    }

    public static Group fromJson(String json, Optional<Group> groupOptional) {
        GroupBuilder builder = Group.builder();
        if (groupOptional.isPresent()) {
            Group existing = groupOptional.get();
            builder.parallelCalls(existing.parallelCalls)
                    .paused(existing.paused)
                    .callbackUrl(existing.callbackUrl)
                    .channelUrl(existing.channelUrl)
                    .name(existing.name)
                    .startingKey(existing.startingKey)
                    .batch(existing.batch)
                    .ttlMinutes(existing.ttlMinutes)
                    .maxWaitMinutes(existing.maxWaitMinutes)
                    .heartbeat(existing.heartbeat);
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
        } catch (IOException e) {
            logger.warn("unable to parse " + json, e);
            throw new RuntimeException(e);
        }
        return builder.build();
    }

    private static Optional<ContentPath> getPrevious(Optional<ContentPath> keyOptional, String channelUrl) {
        ChannelService channelService = HubProvider.getInstance(ChannelService.class);
        String channel = ChannelNameUtils.extractFromChannelUrl(channelUrl);
        Optional<ContentKey> latest = channelService.getLatest(channel, true, false);
        if (latest.isPresent()) {
            DirectionQuery query = DirectionQuery.builder()
                    .channelName(channel)
                    .contentKey(latest.get())
                    .next(false)
                    .count(1)
                    .build();
            SortedSet<ContentKey> keys = channelService.getKeys(query);
            if (keys.isEmpty()) {
                keyOptional = Optional.of(new ContentKey(latest.get().getTime().minusMillis(1), "A"));
            } else {
                keyOptional = Optional.of(keys.first());
            }
        }
        return keyOptional;
    }

    public static Group fromJson(String json) {
        return fromJson(json, Optional.absent());
    }

    /**
     * Returns a Group with all optional values set to the default.
     */
    public Group withDefaults(boolean createKey) {
        Group group = this;
        if (parallelCalls == null) {
            group = group.withParallelCalls(1);
        }
        if (batch == null) {
            group = group.withBatch("SINGLE");
        }
        if (group.isMinute() || group.isSecond()) {
            group = group.withHeartbeat(true);
        }
        if (createKey && getStartingKey() == null) {
            group = group.withStartingKey(GroupStrategy.createContentPath(group));
        }
        if (ttlMinutes == null) {
            group = group.withTtlMinutes(0);
        }
        if (maxWaitMinutes == null) {
            group = group.withMaxWaitMinutes(1);
        }
        return group;
    }

    public boolean isMinute() {
        return MINUTE.equalsIgnoreCase(getBatch());
    }

    public boolean isSecond() {
        return SECOND.equalsIgnoreCase(getBatch());
    }

    public static String getBatchType(boolean single) {
        if (single) {
            return SINGLE;
        }
        return MINUTE;
    }

    @JsonIgnore
    public boolean isNeverStop() {
        return getTtlMinutes() == 0;
    }

    @JsonIgnore
    public boolean isTTL() {
        return getTtlMinutes() > 0;
    }

    public Integer getTtlMinutes() {
        if (ttlMinutes == null) {
            return new Integer(0);
        }
        return ttlMinutes;
    }
}

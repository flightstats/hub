package com.flightstats.hub.group;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flightstats.hub.model.ContentPath;
import com.google.common.base.Optional;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.*;
import lombok.experimental.Wither;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

@Builder
@Getter
@ToString
@EqualsAndHashCode(exclude = {"startingKey"})
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Group {
    private final static Logger logger = LoggerFactory.getLogger(Group.class);

    public static final String SINGLE = "SINGLE";
    public static final String MINUTE = "MINUTE";

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

    private final boolean paused;

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
                || !batch.equals(other.batch);
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
                    .batch(existing.batch);
        }
        try {
            JsonNode root = mapper.readTree(json);
            if (root.has("startItem")) {
                Optional<ContentPath> keyOptional = ContentPath.fromFullUrl(root.get("startItem").asText());
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
        } catch (IOException e) {
            logger.warn("unable to parse " + json, e);
            throw new RuntimeException(e);
        }
        return builder.build();
    }

    public static Group fromJson(String json) {
        return fromJson(json, Optional.absent());
    }

    /**
     * Returns a Group with all optional values set to the default.
     */
    public Group withDefaults() {
        Group group = this;
        if (parallelCalls == null) {
            group = group.withParallelCalls(1);
        }
        if (batch == null) {
            group = group.withBatch("SINGLE");
        }
        if (getStartingKey() == null) {
            group = group.withStartingKey(GroupStrategy.createContentPath(group));
        }
        return group;
    }

    public boolean isMinute() {
        return MINUTE.equalsIgnoreCase(getBatch());
    }

    public static String getBatchType(boolean single) {
        if (single) {
            return SINGLE;
        }
        return MINUTE;
    }
}

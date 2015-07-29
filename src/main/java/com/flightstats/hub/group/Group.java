package com.flightstats.hub.group;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flightstats.hub.model.ContentKey;
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

    private final String callbackUrl;
    private final String channelUrl;
    @Wither
    private final Integer parallelCalls;
    @Wither
    private final String name;
    @Wither
    private final transient ContentKey startingKey;

    private final boolean paused;

    private static final ObjectMapper mapper = new ObjectMapper();

    @JsonIgnore
    public ContentKey getStartingKey() {
        return startingKey;
    }

    public boolean allowedToChange(Group other) {
        return channelUrl.equals(other.channelUrl)
                && name.equals(other.name);
    }

    public boolean isChanged(Group other) {
        return parallelCalls != other.parallelCalls
                || paused != other.paused
                || !callbackUrl.equals(other.callbackUrl);
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
                    .startingKey(existing.startingKey);
        }
        try {
            JsonNode root = mapper.readTree(json);
            if (root.has("startItem")) {
                Optional<ContentKey> keyOptional = ContentKey.fromFullUrl(root.get("startItem").asText());
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
     * Returns a Group with all the defaults set if values aren't set.
     */
    public Group withDefaults() {
        Group group = this;
        if (parallelCalls == null) {
            group = withParallelCalls(1);
        }
        return group;
    }
}

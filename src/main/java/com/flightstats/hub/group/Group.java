package com.flightstats.hub.group;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flightstats.hub.model.ContentKey;
import com.google.common.base.Optional;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.*;
import lombok.experimental.Builder;
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
        return callbackUrl.equals(other.callbackUrl)
                && channelUrl.equals(other.channelUrl)
                && name.equals(other.name);
    }

    private static final Gson gson = new GsonBuilder().create();

    public String toJson() {
        return gson.toJson(this);
    }

    public static Group fromJson(String json) {
        Group group = gson.fromJson(json, GroupBuilder.class).build();
        try {
            JsonNode root = mapper.readTree(json);
            if (root.has("startItem")) {
                String startItem = root.get("startItem").asText();
                Optional<ContentKey> keyOptional = ContentKey.fromFullUrl(startItem);
                if (keyOptional.isPresent()) {
                    group = group.withStartingKey(keyOptional.get());
                }
            }
        } catch (IOException e) {
            logger.warn("unable to parse " + json, e);
        }
        return group;
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

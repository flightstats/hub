package com.flightstats.hub.group;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.flightstats.hub.model.ContentKey;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.*;
import lombok.experimental.Builder;
import lombok.experimental.Wither;

@Builder
@Getter
@ToString
@EqualsAndHashCode(exclude = {"startingKey"})
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Group {

    @NonNull private final String callbackUrl;
    @NonNull private final String channelUrl;
    @Wither
    private final Integer parallelCalls;
    @Wither
    private final String name;
    @Wither
    private final ContentKey startingKey;

    @JsonIgnore
    public ContentKey getStartingKey() {
        return startingKey;
    }

    private static final Gson gson = new GsonBuilder().create();

    public String toJson() {
        return gson.toJson(this);
    }

    public static Group fromJson(String json) {
        return gson.fromJson(json, GroupBuilder.class).build();
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

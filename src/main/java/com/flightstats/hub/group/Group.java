package com.flightstats.hub.group;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.*;
import lombok.experimental.Builder;
import lombok.experimental.Wither;

@Builder
@Getter
@ToString
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Group {

    @NonNull private final String callbackUrl;
    @NonNull private final String channelUrl;
    @Wither
    private final String name;

    private static final Gson gson = new GsonBuilder().create();

    public String toJson() {
        return gson.toJson(this);
    }

    public static Group fromJson(String json) {
        return gson.fromJson(json, GroupBuilder.class).build();
    }
}

package com.flightstats.hub.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.Wither;

import java.util.SortedSet;
import java.util.function.Consumer;

@Builder
@Getter
@AllArgsConstructor
public class StreamResults {
    private final SortedSet<ContentKey> keys;
    private final Consumer<Content> callback;
    private final boolean descending;
    @Wither
    private String channel;

}

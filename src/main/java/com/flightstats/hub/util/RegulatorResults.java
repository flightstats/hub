package com.flightstats.hub.util;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.HashMap;
import java.util.Map;

@Builder
@Getter
@ToString
class RegulatorResults {
    private int threads;
    private long sleepTime;
    @Builder.Default
    private Map<String, Long> slowChannels = new HashMap<>();

    boolean isSlowChannel(String name) {
        return slowChannels.containsKey(name);
    }
}

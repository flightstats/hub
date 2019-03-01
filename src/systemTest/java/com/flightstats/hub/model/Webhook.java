package com.flightstats.hub.model;

import lombok.Builder;

@Builder
public class Webhook {

    private final String callbackUrl;
    private final String channelUrl;
    private final Integer parallelCalls;
    private final String name;
    private final String startItem;
    private final String batch;
    private final boolean heartbeat;
    private final boolean paused;
    private final Integer ttlMinutes;
    private final Integer maxWaitMinutes;
    private final Integer callbackTimeoutSeconds;
    private final boolean fastForwardable;
    private final String tagUrl;
    private final String managedByTag;
    private final Integer maxAttempts;
    private final String errorChannelUrl;

}

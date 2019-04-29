package com.flightstats.hub.dao.aws.writeQueue;

import lombok.Builder;
import lombok.Value;


@Value
@Builder
public class WriteQueueConfig {
    private int threads;
    private int queueSize;
}

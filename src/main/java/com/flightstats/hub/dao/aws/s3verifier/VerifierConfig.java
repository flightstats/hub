package com.flightstats.hub.dao.aws.s3verifier;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.Wither;

import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Value
@Builder
@Wither
public class VerifierConfig {
    boolean enabled;

    long baseTimeoutValue;
    TimeUnit baseTimeoutUnit;

    int offsetMinutes;

    int channelThreads;
    int queryThreads;

    Function<String, String> endpointUrlGenerator;
}

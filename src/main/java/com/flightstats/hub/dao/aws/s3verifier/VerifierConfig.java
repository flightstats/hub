package com.flightstats.hub.dao.aws.s3verifier;

import lombok.Builder;
import lombok.Value;

import java.util.function.Function;

@Value
@Builder
public class VerifierConfig {
    boolean enabled;

    long baseTimeoutMinutes;
    int offsetMinutes;

    int channelThreads;
    int queryThreads;

    Function<String, String> endpointUrlGenerator;
}

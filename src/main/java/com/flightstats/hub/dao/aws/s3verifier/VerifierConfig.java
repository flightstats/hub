package com.flightstats.hub.dao.aws.s3Verifier;

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

    public String getChannelVerifierEndpoint(String channelName) {
        return getEndpointUrlGenerator().apply(channelName);
    }
}

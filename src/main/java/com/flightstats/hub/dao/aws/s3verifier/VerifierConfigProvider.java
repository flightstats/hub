package com.flightstats.hub.dao.aws.s3Verifier;

import com.flightstats.hub.config.AppProperties;
import com.flightstats.hub.config.S3Properties;
import com.google.inject.Provider;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

public class VerifierConfigProvider implements Provider<VerifierConfig> {

    private final AppProperties appProperties;
    private final S3Properties s3Properties;

    @Inject
    public VerifierConfigProvider(AppProperties appProperties, S3Properties s3Properties){
        this.appProperties = appProperties;
        this.s3Properties = s3Properties;
    }

    @Override
    public VerifierConfig get() {
        return VerifierConfig.builder()
                .enabled(s3Properties.getVerifierRun())
                .baseTimeoutValue(s3Properties.getVerifierBaseTimeoutInMins())
                .baseTimeoutUnit(TimeUnit.MINUTES)
                .offsetMinutes(s3Properties.getVerifierOffsetInInMins())
                .channelThreads(s3Properties.getVerifierChannelThreads())
                .queryThreads(s3Properties.getVerifierChannelThreads() * 2)
                .endpointUrlGenerator(channelName -> appProperties.getAppUrl() + "internal/s3Verifier/" + channelName)
                .build();
    }
}

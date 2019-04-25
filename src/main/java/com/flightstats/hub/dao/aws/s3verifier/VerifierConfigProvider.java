package com.flightstats.hub.dao.aws.s3Verifier;

import com.flightstats.hub.config.AppProperty;
import com.flightstats.hub.config.S3Property;
import com.google.inject.Provider;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

public class VerifierConfigProvider implements Provider<VerifierConfig> {

    private AppProperty appProperty;
    private S3Property s3Property;

    @Inject
    public VerifierConfigProvider(AppProperty appProperty, S3Property s3Property){
        this.appProperty = appProperty;
        this.s3Property = s3Property;
    }

    @Override
    public VerifierConfig get() {
        return VerifierConfig.builder()
                .enabled(s3Property.getVerifierRun())
                .baseTimeoutValue(s3Property.getVerifierBaseTimeoutInMins())
                .baseTimeoutUnit(TimeUnit.MINUTES)
                .offsetMinutes(s3Property.getVerifierOffsetInInMins())
                .channelThreads(s3Property.getVerifierChannelThreads())
                .queryThreads(s3Property.getVerifierChannelThreads() * 2)
                .endpointUrlGenerator(channelName -> appProperty.getAppUrl() + "internal/s3Verifier/" + channelName)
                .build();
    }
}

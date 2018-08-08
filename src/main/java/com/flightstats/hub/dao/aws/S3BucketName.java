package com.flightstats.hub.dao.aws;

import com.flightstats.hub.app.HubProperties;
import com.google.inject.Inject;
import com.google.inject.name.Named;

public class S3BucketName {
    private final String legacyS3BucketName;

    @Inject
    public S3BucketName(@Named("s3.environment") String environment, @Named("app.name") String appName) {
        this.legacyS3BucketName = appName + "-" + environment;
    }

    public String getS3BucketName() {
        return HubProperties.getProperty("s3.bucket_name", legacyS3BucketName);
    }

}

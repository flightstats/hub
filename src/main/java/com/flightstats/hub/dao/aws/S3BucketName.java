package com.flightstats.hub.dao.aws;

import com.google.inject.Inject;
import com.google.inject.name.Named;

public class S3BucketName {
    private final String s3BucketName;

    @Inject
    public S3BucketName(@Named("s3.environment") String environment, @Named("app.name") String appName) {
        this.s3BucketName = appName + "-" + environment;
    }

    public String getS3BucketName() {
        return s3BucketName;
    }

}

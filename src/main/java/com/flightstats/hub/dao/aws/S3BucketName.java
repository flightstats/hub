package com.flightstats.hub.dao.aws;

import com.flightstats.hub.config.AppProperty;
import com.flightstats.hub.config.S3Property;

import javax.inject.Inject;


public class S3BucketName {

    private final String legacyS3BucketName;
    private S3Property s3Property;

    @Inject
    public S3BucketName(AppProperty appProperty, S3Property s3Property) {
        this.legacyS3BucketName = appProperty.getAppName() + "-" + s3Property.getEnv();
        this.s3Property = s3Property;
    }

    public String getS3BucketName() {
        return s3Property.getBucketName(legacyS3BucketName);
    }

}

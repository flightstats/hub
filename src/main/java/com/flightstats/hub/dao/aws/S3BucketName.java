package com.flightstats.hub.dao.aws;

import com.flightstats.hub.config.properties.AppProperties;
import com.flightstats.hub.config.properties.S3Properties;

import javax.inject.Inject;


public class S3BucketName {

    private final String legacyS3BucketName;
    private final S3Properties s3Properties;

    @Inject
    public S3BucketName(AppProperties appProperties, S3Properties s3Properties) {
        this.legacyS3BucketName = appProperties.getAppName() + "-" + s3Properties.getEnv();
        this.s3Properties = s3Properties;
    }

    public String getS3BucketName() {
        return s3Properties.getBucketName(legacyS3BucketName);
    }

}

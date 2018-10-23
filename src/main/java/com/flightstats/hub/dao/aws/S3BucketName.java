package com.flightstats.hub.dao.aws;

import com.flightstats.hub.app.HubProperties;
import lombok.Value;

import javax.inject.Inject;

// TODO: i don't think we need this level of abstraction. just use the config object.

@Value
public class S3BucketName {

    private final String s3BucketName;

    @Inject
    public S3BucketName(HubProperties hubProperties) {
        String appName = hubProperties.getProperty("app.name");
        String environment = hubProperties.getProperty("s3.environment");
        String legacyS3BucketName = appName + "-" + environment;
        s3BucketName = hubProperties.getProperty("s3.bucket_name", legacyS3BucketName);
    }

}

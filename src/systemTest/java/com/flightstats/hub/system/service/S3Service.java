package com.flightstats.hub.system.service;

import com.flightstats.hub.clients.s3.S3ClientFactory;
import com.flightstats.hub.clients.s3.S3ItemResource;
import com.google.inject.Inject;
import okhttp3.HttpUrl;

public class S3Service {
    private S3ItemResource s3ItemResource;
    private HttpUrl s3Url;

    @Inject
    public S3Service(S3ClientFactory s3ClientFactory) {
        this.s3ItemResource = s3ClientFactory.getS3Client(S3ItemResource.class);
        this.s3Url = s3ClientFactory.getBaseUrl();
    }

    public String getItemUrl(String path) {
        return s3Url + "callback/" + path;
    }
}

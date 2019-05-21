package com.flightstats.hub.clients.s3;

import com.flightstats.hub.model.BucketName;
import okhttp3.HttpUrl;
import retrofit2.Retrofit;

import javax.inject.Inject;
import javax.inject.Named;

public class S3ClientFactory {
    private final Retrofit retrofitS3;
    private final String bucketName;

    @Inject
    public S3ClientFactory(@Named("s3") Retrofit retrofitS3, @Named("release.name") String releaseName) {
        this.retrofitS3 = retrofitS3;
        this.bucketName = BucketName
                .builder()
                .releaseName(releaseName)
                .build()
                .getFullName();
    }

    public HttpUrl getBaseUrl() {
        return retrofitS3.baseUrl();
    }

    public <T> T getS3Client(Class<T> serviceClass) {
        return retrofitS3.create(serviceClass);
    }

    public String getBucketName() {
        return bucketName;
    }
}

package com.flightstats.hub.client;

import retrofit2.Retrofit;

import javax.inject.Inject;
import javax.inject.Named;

public class S3ClientFactory {
    private final Retrofit retrofitS3;

    @Inject
    public S3ClientFactory(@Named("S3") Retrofit retrofitS3) {
        this.retrofitS3 = retrofitS3;
    }

    Retrofit getRetrofitS3() {
        return retrofitS3;
    }
}

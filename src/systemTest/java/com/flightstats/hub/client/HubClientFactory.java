package com.flightstats.hub.client;

import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import retrofit2.Retrofit;

import javax.inject.Inject;
import javax.inject.Named;

@Slf4j
public class HubClientFactory {

    private final Retrofit retrofitHub;

    @Inject
    public HubClientFactory(@Named("hub") Retrofit retrofitHub) {
        this.retrofitHub = retrofitHub;

    }

    public <T> T getHubClient(Class<T> serviceClass) {
        return this.retrofitHub.create(serviceClass);
    }

    public HttpUrl getHubBaseUrl() {
        return retrofitHub.baseUrl();
    }

}

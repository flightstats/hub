package com.flightstats.hub.client;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import retrofit2.Retrofit;

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

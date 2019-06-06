package com.flightstats.hub.clients.callback;

import okhttp3.HttpUrl;
import retrofit2.Retrofit;

import javax.inject.Inject;
import javax.inject.Named;

public class CallbackClientFactory {

    private final Retrofit retrofitHub;

    @Inject
    public CallbackClientFactory(@Named("callback") Retrofit retrofitHub) {
        this.retrofitHub = retrofitHub;
    }

    public <T> T getCallbackClient(Class<T> serviceClass) {
        return this.retrofitHub.create(serviceClass);
    }

    public HttpUrl getCallbackUrl() {
        return retrofitHub.baseUrl();
    }
}

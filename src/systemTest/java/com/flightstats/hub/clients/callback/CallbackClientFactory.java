package com.flightstats.hub.clients.callback;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import okhttp3.HttpUrl;
import retrofit2.Retrofit;

public class CallbackClientFactory {

    private Retrofit retrofitHub;

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

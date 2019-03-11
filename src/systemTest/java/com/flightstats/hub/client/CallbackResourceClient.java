package com.flightstats.hub.client;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface CallbackResourceClient {

    @GET("/callback/{webhookName}")
    Call<String> get(@Path("webhookName") String webhookName);
}

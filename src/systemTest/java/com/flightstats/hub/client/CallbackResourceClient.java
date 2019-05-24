package com.flightstats.hub.client;

import com.flightstats.hub.model.WebhookCallback;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface CallbackResourceClient {
    @GET("/callback/{webhookName}")
    Call<WebhookCallback> get(@Path("webhookName") String webhookName);

}

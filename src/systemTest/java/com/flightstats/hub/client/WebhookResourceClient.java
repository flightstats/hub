package com.flightstats.hub.client;

import com.flightstats.hub.model.Webhook;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface WebhookResourceClient {

    @PUT("/webhook/{webhookName}")
    Call<Webhook> create(@Path("webhookName") String webhookName, @Body Webhook webhook);

    @GET("/webhook/{webhookName}")
    Call<Webhook> get(@Path("webhookName") String webhookName);

    @GET("/webhook/{webhookName}/errors")
    Call<Object> getError(@Path("webhookName") String webhookName);

    @DELETE("/webhook/{webhookName}")
    Call<Void> delete(@Path("webhookName") String webhookName);
}


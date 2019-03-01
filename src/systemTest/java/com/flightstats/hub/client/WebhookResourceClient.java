package com.flightstats.hub.client;

import com.flightstats.hub.model.Webhook;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface WebhookResourceClient {

    @PUT("/webhook/{webhookname}")
    Call<Webhook> create(@Path("webhookname") String webhookname, @Body Webhook webhook);

    @GET("/webhook/{webhookname}")
    Call<Webhook> get(@Path("webhookname") String webhookname);

    @DELETE("/webhook/{webhookname}")
    Call<Void> delete(@Path("webhookname") String webhookname);
}


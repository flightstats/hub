package com.flightstats.hub.client;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;
import retrofit2.http.Url;

public interface CallbackResourceClient {

    @GET
    Call<String> get(@Url String hostUrl, @Query("webhookname") String webhookName);
}

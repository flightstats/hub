package com.flightstats.hub.clients.hub.channel;

import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.ChannelConfig;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface ChannelResourceClient {

    @POST("/channel")
    Call<Object> create(@Body ChannelConfig channel);

    @PUT("/channel/{channelName}")
    Call<Object> update(@Path("channelName") String channelName, @Body ChannelConfig channel);

    @GET("/channel/{channelName}")
    Call<ContentKey> get(@Path("channelName") String channelName);

    @DELETE("/channel/{channelName}")
    Call<Void> delete(@Path("channelName") String channelName);
}


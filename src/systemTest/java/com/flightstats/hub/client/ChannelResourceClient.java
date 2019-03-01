package com.flightstats.hub.client;

import com.flightstats.hub.model.Channel;
import com.flightstats.hub.model.ContentKey;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface ChannelResourceClient {

    @POST("/channel")
    Call<Object> create(@Body Channel channel);

    @GET("/channel/{channelname}")
    Call<ContentKey> get(@Path("channelname") String channelname);

    @DELETE("/channel/{channelname}")
    Call<Void> delete(@Path("channelname") String channelname);
}


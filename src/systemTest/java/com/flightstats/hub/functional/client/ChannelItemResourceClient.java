package com.flightstats.hub.functional.client;

import com.flightstats.hub.functional.model.ChannelItem;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface ChannelItemResourceClient {

    @POST("/channel/{channelName}")
    Call<ChannelItem> add(@Path("channelName") String channelName,
                          @Body Object item);

    @GET("/channel/{channelName}/{Y}/{M}/{D}/{h}/{m}/{s}/{ms}/{hash}")
    Call<Object> get(@Path("channelName") String channelName,
                     @Path("Y") int year,
                     @Path("M") int month,
                     @Path("D") int day,
                     @Path("h") int hour,
                     @Path("m") int minute,
                     @Path("s") int second,
                     @Path("ms") int millis,
                     @Path("hash") String hash);
}

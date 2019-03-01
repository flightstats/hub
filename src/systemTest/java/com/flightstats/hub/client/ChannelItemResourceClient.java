package com.flightstats.hub.client;

import com.flightstats.hub.model.ChannelItem;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface ChannelItemResourceClient {

    @POST("/channel/{channelname}")
    Call<ChannelItem> add(@Path("channelname") String channelName,
                          @Body Object item);

    @GET("/channel/{channelname}/{Y}/{M}/{D}/{h}/{m}/{s}/{ms}/{hash}")
    Call<Object> get(@Path("channelname") String channelname,
                     @Path("Y") int year,
                     @Path("M") int month,
                     @Path("D") int day,
                     @Path("h") int hour,
                     @Path("m") int minute,
                     @Path("s") int second,
                     @Path("ms") int millis,
                     @Path("hash") String hash);
}

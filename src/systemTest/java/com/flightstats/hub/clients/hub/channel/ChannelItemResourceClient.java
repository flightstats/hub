package com.flightstats.hub.clients.hub.channel;

import com.flightstats.hub.model.ChannelItem;
import com.flightstats.hub.model.Location;
import com.flightstats.hub.model.TimeQuery;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

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

    @GET("/channel/{channelName}/{Y}/{M}/{D}/{h}/{m}/{s}")
    Call<TimeQuery> getItemsSecondsPath(@Path("channelName") String channelName,
                                        @Path("Y") int year,
                                        @Path("M") int month,
                                        @Path("D") int day,
                                        @Path("h") int hour,
                                        @Path("m") int minute,
                                        @Path("s") int second,
                                        @Query("location") Location location);
}

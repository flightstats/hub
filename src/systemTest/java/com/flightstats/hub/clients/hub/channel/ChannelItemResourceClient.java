package com.flightstats.hub.clients.hub.channel;

import com.flightstats.hub.model.ChannelItem;
import com.flightstats.hub.model.ChannelItemQueryDirection;
import com.flightstats.hub.model.Location;
import com.flightstats.hub.model.TimeQueryResult;
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

    @POST("/channel/{channelName}/{historicalPath}")
    Call<ChannelItem> addHistorical(@Path("channelName") String channelName,
                               @Path(value="historicalPath", encoded=true) String historicalPath,
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
    Call<TimeQueryResult> getItemsSecondsPath(@Path("channelName") String channelName,
                                              @Path("Y") int year,
                                              @Path("M") int month,
                                              @Path("D") int day,
                                              @Path("h") int hour,
                                              @Path("m") int minute,
                                              @Path("s") int second,
                                              @Query("location") Location location);

    @GET("/channel/{itemPath}/{direction}/{numberOfItems}")
    Call<TimeQueryResult> getDirectionalItems(@Path(value="itemPath", encoded=true) String itemPath,
                                              @Path("direction") ChannelItemQueryDirection direction,
                                              @Path("numberOfItems") int numberOfItems);

    @GET("/channel/{itemPath}/{direction}")
    Call<Object> getDirectionalItem(@Path(value="itemPath", encoded=true) String itemPath,
                                    @Path("direction") ChannelItemQueryDirection direction);
}

package com.flightstats.hub.clients.hub.channel;

import com.flightstats.hub.model.ChannelItem;
import com.flightstats.hub.model.ChannelItemQueryDirection;
import com.flightstats.hub.model.Location;
import com.flightstats.hub.model.TimeQuery;
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

    @GET("/channel/{channelName}/{year}/{month}/{day}/{hour}/{minute}/{second}/{millis}/{hash}")
    Call<Object> get(@Path("channelName") String channelName,
                     @Path("year") int year,
                     @Path("month") int month,
                     @Path("day") int day,
                     @Path("hour") int hour,
                     @Path("minute") int minute,
                     @Path("second") int second,
                     @Path("millis") int millis,
                     @Path("hash") String hash);

    @GET("/channel/{channelName}/{year}/{month}/{day}")
    Call<TimeQueryResult> getItemForTimeFromLocation(@Path("channelName") String channelName,
                                               @Path("year") int year,
                                               @Path("month") int month,
                                               @Path("day") int day,
                                               @Query("location") Location location);


    @GET("/channel/{itemPath}/{direction}/{numberOfItems}")
    Call<TimeQueryResult> getDirectionalItems(@Path(value="itemPath", encoded=true) String itemPath,
                                              @Path("direction") ChannelItemQueryDirection direction,
                                              @Path("numberOfItems") int numberOfItems);

    @GET("/channel/{itemPath}/{direction}")
    Call<Object> getDirectionalItem(@Path(value="itemPath", encoded=true) String itemPath,
                                    @Path("direction") ChannelItemQueryDirection direction);

}

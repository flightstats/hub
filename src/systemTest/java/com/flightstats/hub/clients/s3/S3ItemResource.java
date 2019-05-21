package com.flightstats.hub.clients.s3;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface S3ItemResource {
    @GET("/{bucketName}/{channelName}/{Y}/{M}/{D}/{h}/{m}/{s}/{ms}/{hash}")
    Call<Object> get(@Path("bucketName") String bucketName,
                     @Path("channelName") String channelName,
                     @Path("Y") int year,
                     @Path("M") int month,
                     @Path("D") int day,
                     @Path("h") int hour,
                     @Path("m") int minute,
                     @Path("s") int second,
                     @Path("ms") int millis,
                     @Path("hash") String hash);
}

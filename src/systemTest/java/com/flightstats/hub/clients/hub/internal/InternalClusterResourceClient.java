package com.flightstats.hub.clients.hub.internal;

import com.flightstats.hub.model.InternalCluster;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface InternalClusterResourceClient {
    @GET("/internal/cluster")
    Call<InternalCluster> get();

    @POST("/internal/cluster/recommission/{node}")
    Call<Object> recommission(@Path("node") String node);
}

package com.flightstats.hub.clients.hub.zookeeper;

import com.flightstats.hub.model.ZookeeperNode;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface ChannelLatestResourceClient {
    @GET("/internal/zookeeper/ChannelLatestUpdated")
    Call<ZookeeperNode> getAll();

    @GET("/internal/zookeeper/ChannelLatestUpdated/{channelName}")
    Call<ZookeeperNode> getChannel(@Path("channelName") String channelName);
}

package com.flightstats.hub.system.service;

import com.flightstats.hub.clients.hub.HubClientFactory;
import com.flightstats.hub.clients.hub.zookeeper.ChannelLatestResourceClient;
import com.flightstats.hub.model.ZookeeperNode;
import com.flightstats.hub.model.ZookeeperNodeData;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import retrofit2.Call;
import retrofit2.Response;

import javax.inject.Inject;
import javax.ws.rs.core.Response.Status;

import java.util.Optional;

import static junit.framework.Assert.assertEquals;

@Slf4j
public class InternalZookeeperService {
    private final ChannelLatestResourceClient channelLatestClient;

    @Inject
    public InternalZookeeperService(HubClientFactory hubClientFactory) {
        this.channelLatestClient = hubClientFactory.getHubClient(ChannelLatestResourceClient.class);
    }

    @SneakyThrows
    public ZookeeperNode getAllChannelsWithLatestUpdated() {
        Call<ZookeeperNode> call = channelLatestClient.getAll();
        Response<ZookeeperNode> response = call.execute();
        log.debug("zookeeper ChannelLatestUpdate list retrieved: {}", response);
        assertEquals(Status.OK.getStatusCode(), response.code());

        return response.body();
    }

    @SneakyThrows
    public Optional<String> getChannelLatestUpdated(String channelName) {
        Call<ZookeeperNode> call = channelLatestClient.getChannel(channelName);
        Response<ZookeeperNode> response = call.execute();
        log.debug("zookeeper ChannelLatestUpdate for {} retrieved: {}", response);

        return Optional.ofNullable(response.body())
                .filter(body -> response.isSuccessful())
                .map(ZookeeperNode::getData)
                .map(ZookeeperNodeData::getString);
    }
}

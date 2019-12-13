package com.flightstats.hub.system.service;

import com.flightstats.hub.clients.hub.HubClientFactory;
import com.flightstats.hub.clients.hub.internal.ChannelMaxItemsResourceClient;
import com.flightstats.hub.clients.hub.internal.InternalClusterResourceClient;
import com.flightstats.hub.clients.hub.internal.InternalPropertiesResourceClient;
import com.flightstats.hub.model.InternalCluster;
import com.flightstats.hub.model.InternalProperties;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import retrofit2.Call;
import retrofit2.Response;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
public class HubInternalService {
    private final ChannelMaxItemsResourceClient channelMaxItemsResourceClient;
    private final InternalPropertiesResourceClient internalPropertiesResourceClient;
    private final InternalClusterResourceClient internalClusterResourceClient;

    @Inject
    public HubInternalService(HubClientFactory hubClientFactory) {
        this.channelMaxItemsResourceClient = hubClientFactory.getHubClient(ChannelMaxItemsResourceClient.class);
        this.internalPropertiesResourceClient = hubClientFactory.getHubClient(InternalPropertiesResourceClient.class);
        this.internalClusterResourceClient = hubClientFactory.getHubClient(InternalClusterResourceClient.class);
    }

    @SneakyThrows
    public void enforceMaxItems(String channelName) {
        try {
            Call<Void> call = channelMaxItemsResourceClient.enforceMaxItems(channelName);
            Response<Void> response = call.execute();
            log.info("max items enforcer response status: " + response.code());
        } catch (Exception e) {
            log.error("error max items enforcer response: ", e.getMessage());
            throw e;
        }
    }

    public boolean hasServerName(String server) {
        Call<InternalProperties> propertiesCall = internalPropertiesResourceClient.get();
        try {
            Response<InternalProperties> response = propertiesCall.execute();
            Optional<InternalProperties> optionalResponse = Optional.ofNullable(response.body());
            return optionalResponse.filter(res -> res.getServer() != null)
                    .get().getServer().contains(server);
        } catch (Exception e) {
            log.error(e.getMessage());
            return false;
        }
    }

    @SneakyThrows
    public List<String> getDoNotStartStateNodeIPs() {
        Call<InternalCluster> clusterCall = internalClusterResourceClient.get();
        Response<InternalCluster> response = clusterCall.execute();
        Optional<InternalCluster> optionalResponse = Optional.ofNullable(response.body());
        return optionalResponse.map(InternalCluster::getDoNotStartServers).orElse(new ArrayList<>());
    }

    public boolean recommissionNode(String node) {
        try {
            Call<Object> recomCall = internalClusterResourceClient.recommission(node);
            Response<Object> response = recomCall.execute();
            return response.code() == 202;
        } catch (Exception e) {
            log.info(e.getMessage());
            return false;
        }
    }

}

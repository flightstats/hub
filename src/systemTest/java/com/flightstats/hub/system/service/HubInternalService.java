package com.flightstats.hub.system.service;

import com.flightstats.hub.clients.hub.HubClientFactory;
import com.flightstats.hub.clients.hub.internal.ChannelMaxItemsResourceClient;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import retrofit2.Call;
import retrofit2.Response;

import javax.inject.Inject;

@Slf4j
public class HubInternalService {
    private final ChannelMaxItemsResourceClient channelMaxItemsResourceClient;


    @Inject
    public HubInternalService(HubClientFactory hubClientFactory) {
        this.channelMaxItemsResourceClient = hubClientFactory.getHubClient(ChannelMaxItemsResourceClient.class);
    }

    @SneakyThrows
    public void enforceMaxItems(String channelName) {
        try {
            Call<Void> call = channelMaxItemsResourceClient.enforceMaxItems(channelName);
            Response<Void> response = call.execute();
            log.info("max items enforcer response status: " + response.code());
        } catch (Exception e) {
            log.warn("error max items enforcer response: ", e.getMessage());
            throw e;
        }
    }
}

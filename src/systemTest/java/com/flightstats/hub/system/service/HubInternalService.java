package com.flightstats.hub.system.service;

import com.flightstats.hub.clients.hub.HubClientFactory;
import com.flightstats.hub.clients.hub.internal.ChannelMaxItemsResourceClient;
import com.flightstats.hub.clients.hub.internal.InternalPropertiesResourceClient;
import com.flightstats.hub.model.InternalProperties;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import retrofit2.Call;
import retrofit2.Response;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;

@Slf4j
public class HubInternalService {
    private final ChannelMaxItemsResourceClient channelMaxItemsResourceClient;
    private final InternalPropertiesResourceClient internalPropertiesResourceClient;

    @Inject
    public HubInternalService(HubClientFactory hubClientFactory) {
        this.channelMaxItemsResourceClient = hubClientFactory.getHubClient(ChannelMaxItemsResourceClient.class);
        this.internalPropertiesResourceClient = hubClientFactory.getHubClient(InternalPropertiesResourceClient.class);
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

    @SneakyThrows
    public boolean hasServerName(String server) {
        Call<InternalProperties> propertiesCall = internalPropertiesResourceClient.get();
        Response<InternalProperties> response = propertiesCall.execute();
        Optional<InternalProperties> optionalResponse = Optional.ofNullable(response.body());
        if (optionalResponse.isPresent()) {
            return optionalResponse.get().getServers().stream()
                    .anyMatch((name) -> name.contains(server));

        } else {
            log.info("!!!!!!!!!!!! {}", response.toString());
        }
        return false;
    }
}

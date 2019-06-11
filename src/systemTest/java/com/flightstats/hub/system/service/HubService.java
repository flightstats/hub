package com.flightstats.hub.system.service;

import com.flightstats.hub.clients.hub.HubClientFactory;
import com.flightstats.hub.clients.hub.hub.InternalResourceClient;
import com.flightstats.hub.model.Internal;
import lombok.extern.slf4j.Slf4j;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;

import javax.inject.Inject;
import java.util.Optional;

@Slf4j
public class HubService {

    private final InternalResourceClient internalResourceClient;

    @Inject
    public HubService(HubClientFactory hubClientFactory) {
        this.internalResourceClient = hubClientFactory.getHubClient(InternalResourceClient.class);
    }

    public boolean getHubHealth() {
        try {
            Call<ResponseBody> call = internalResourceClient.getHealth();
            int statusCode = call.execute().code();
            log.info("############################statusCode {}", statusCode);
            return statusCode == 200;
        } catch (Exception e) {
            log.warn("error reaching hub health endpoint {}", e.getMessage());
            return false;
        }

    }

    public int getServerCount() {
        try {
            Call<Internal> call = internalResourceClient.getProperties();
            Optional<Internal> optionalResponseBody = Optional.ofNullable(call.execute().body());
            optionalResponseBody.ifPresent(internal -> log.info("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@ servers {}", internal.getServers().length));
            return optionalResponseBody.map(internal -> internal.getServers().length).orElse(0);
        } catch (Exception e) {
            log.warn("unable to reach hub internal endpoint {}", e.getMessage());
            return 0;
        }
    }

}

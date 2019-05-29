package com.flightstats.hub.app;

import com.flightstats.hub.cluster.Cluster;
import com.flightstats.hub.config.properties.AppProperties;
import com.flightstats.hub.config.properties.LocalHostProperties;
import com.flightstats.hub.rest.RestClient;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;

@Singleton
@Slf4j
public class AppUrlCheck extends AbstractIdleService {

    private final Client client = RestClient.createClient(
            1000,
            1000,
            true,
            true);

    private final Cluster cluster;
    private final AppProperties appProperties;
    private final LocalHostProperties localHostProperties;

    @Inject
    public AppUrlCheck(@Named("HubCluster") Cluster cluster,
                       AppProperties appProperties,
                       LocalHostProperties localHostProperties) {
        this.cluster = cluster;
        this.appProperties = appProperties;
        this.localHostProperties = localHostProperties;
        HubServices.register(this);
    }


    @Override
    protected void startUp() {
        if (hasHealthyServers()) {
            String appUrl = appProperties.getAppUrl();
            ClientResponse response = client.resource(appUrl).get(ClientResponse.class);
            log.info("got response {}", response);
            if (response.getStatus() != 200) {
                String msg = "unable to connect to app.url " + appUrl + " status=" + response.getStatus();
                log.error(msg);
                throw new RuntimeException(msg);
            }
        } else {
            log.info("no servers to test");
        }
    }

    private boolean hasHealthyServers() {
        for (String server : cluster.getAllServers()) {
            String serverUri = localHostProperties.getScheme() + server;
            if (!serverUri.equals(localHostProperties.getUriWithHostName())) {
                ClientResponse response = client.resource(serverUri + "/health").get(ClientResponse.class);
                log.info("got response {}", response);
                if (response.getStatus() == 200) {
                    return true;
                }
            } else {
                log.info("ignoring {}", serverUri);
            }
        }
        return false;
    }

    @Override
    protected void shutDown() {
        //do nothing
    }
}

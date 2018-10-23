package com.flightstats.hub.app;

import com.flightstats.hub.cluster.CuratorCluster;
import com.google.common.util.concurrent.AbstractIdleService;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
class AppUrlCheck extends AbstractIdleService {

    private final static Logger logger = LoggerFactory.getLogger(AppUrlCheck.class);

    private final CuratorCluster curatorCluster;
    private final Client httpClient;
    private final HubProperties hubProperties;

    @Inject
    public AppUrlCheck(@Named("HubCluster") CuratorCluster curatorCluster, Client httpClient, HubProperties hubProperties) {
        this.curatorCluster = curatorCluster;
        this.httpClient = httpClient;
        this.hubProperties = hubProperties;
        HubServices.register(this);
    }

    @Override
    protected void startUp() {
        if (hasHealthyServers()) {
            String appUrl = hubProperties.getAppUrl();
            ClientResponse response = httpClient.resource(appUrl).get(ClientResponse.class);
            logger.info("got response {}", response);
            if (response.getStatus() != 200) {
                String msg = "unable to connect to app.url " + appUrl + " status=" + response.getStatus();
                logger.error(msg);
                throw new RuntimeException(msg);
            }
        } else {
            logger.info("no servers to test");
        }
    }

    private boolean hasHealthyServers() {
        for (String server : curatorCluster.getAllServers()) {
            String serverUri = HubHost.getScheme() + server;
            if (!serverUri.equals(HubHost.getLocalHttpNameUri())) {
                ClientResponse response = httpClient.resource(serverUri + "/health").get(ClientResponse.class);
                logger.info("got response {}", response);
                if (response.getStatus() == 200) {
                    return true;
                }
            } else {
                logger.info("ignoring {}", serverUri);
            }
        }
        return false;
    }

    @Override
    protected void shutDown() throws Exception {
        //do nothing
    }
}

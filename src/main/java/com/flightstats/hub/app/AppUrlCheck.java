package com.flightstats.hub.app;

import com.flightstats.hub.cluster.Cluster;
import com.flightstats.hub.rest.RestClient;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
class AppUrlCheck extends AbstractIdleService {

    private final static Logger logger = LoggerFactory.getLogger(AppUrlCheck.class);

    @Inject
    @Named("HubCluster")
    private Cluster cluster;

    private Client client = RestClient.createClient(1000, 1000, true, true);

    public AppUrlCheck() {
        HubServices.register(this);
    }

    @Override
    protected void startUp() throws Exception {
        if (hasHealthyServers()) {
            String appUrl = HubProperties.getAppUrl();
            ClientResponse response = client.resource(appUrl).get(ClientResponse.class);
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
        for (String server : cluster.getAllServers()) {
            String serverUri = HubHost.getScheme() + server;
            if (!serverUri.equals(HubHost.getLocalHttpNameUri())) {
                ClientResponse response = client.resource(serverUri + "/health").get(ClientResponse.class);
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

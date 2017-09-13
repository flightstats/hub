package com.flightstats.hub.app;

import com.flightstats.hub.cluster.Cluster;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

@Singleton
class AppUrlCheck extends AbstractIdleService {

    private final static Logger logger = LoggerFactory.getLogger(AppUrlCheck.class);

    @Inject
    @Named("HubCluster")
    private Cluster cluster;

    @Inject
    private Client client;

    public AppUrlCheck() {
        HubServices.register(this);
    }

    @Override
    protected void startUp() throws Exception {
        if (getValidServers().isEmpty()) {
            logger.info("no servers to test");
        } else {
            String appUrl = HubProperties.getAppUrl();
            ClientResponse response = client.resource(appUrl).get(ClientResponse.class);
            logger.info("got response {}", response);
            if (response.getStatus() != 200) {
                String msg = "unable to connect to app.url " + appUrl + " status=" + response.getStatus();
                logger.error(msg);
                throw new RuntimeException(msg);
            }
        }
    }

    private Set<String> getValidServers() {
        Set<String> validServers = new HashSet<>();
        cluster.getAllServers().forEach(server -> {
            ClientResponse response = client
                    .resource(HubHost.getScheme() + server + "/health")
                    .get(ClientResponse.class);
            logger.info("got response {}", response);
            if (response.getStatus() == 200) {
                validServers.add(server);
            }
        });
        return validServers;
    }

    @Override
    protected void shutDown() throws Exception {
        //do nothing
    }
}

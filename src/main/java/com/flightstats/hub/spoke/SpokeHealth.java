package com.flightstats.hub.spoke;

import com.flightstats.hub.app.HubHost;
import com.flightstats.hub.app.HubServices;
import com.flightstats.hub.cluster.CuratorCluster;
import com.flightstats.hub.rest.RestClient;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Inject;
import com.sun.jersey.api.client.ClientResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpokeHealth {

    private final static Logger logger = LoggerFactory.getLogger(SpokeHealth.class);
    private final RemoteSpokeStore remoteSpokeStore;

    @Inject
    public SpokeHealth(RemoteSpokeStore remoteSpokeStore) {
        this.remoteSpokeStore = remoteSpokeStore;
        HubServices.register(new SpokeHealthHook(), HubServices.TYPE.INITIAL_POST_START);
    }

    private class SpokeHealthHook extends AbstractIdleService {

        @Override
        protected void startUp() throws Exception {
            ClientResponse health = RestClient.defaultClient()
                    .resource(HubHost.getLocalHttpIpUri() + "/health")
                    .get(ClientResponse.class);
            logger.info("localhost health {}", health);
            remoteSpokeStore.testOne(CuratorCluster.getLocalServer());
            if (!remoteSpokeStore.testAll()) {
                logger.warn("unable to cleanly start Spoke");
                throw new RuntimeException("unable to cleanly start Spoke");
            }
        }

        @Override
        protected void shutDown() throws Exception {
        }
    }

}

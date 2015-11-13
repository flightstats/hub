package com.flightstats.hub.spoke;

import com.flightstats.hub.app.HubServices;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Inject;

public class SpokeClusterRegister {

    @Inject
    private CuratorSpokeCluster curatorSpokeCluster;

    public SpokeClusterRegister() {
        HubServices.register(new CuratorSpokeClusterHook(), HubServices.TYPE.FINAL_POST_START, HubServices.TYPE.PRE_STOP);
    }

    private class CuratorSpokeClusterHook extends AbstractIdleService {
        @Override
        protected void startUp() throws Exception {
            curatorSpokeCluster.addCacheListener();
            curatorSpokeCluster.register();
        }

        @Override
        protected void shutDown() throws Exception {
            curatorSpokeCluster.delete();
        }
    }
}

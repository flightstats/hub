package com.flightstats.hub.cluster;

import com.flightstats.hub.app.HubServices;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

@Singleton
public class HubClusterRegister {

    @Inject
    @Named("HubCuratorCluster")
    private CuratorCluster hubCuratorCluster;

    public HubClusterRegister() {
        HubServices.register(new CuratorClusterHook(), HubServices.TYPE.FINAL_POST_START, HubServices.TYPE.PRE_STOP);
    }

    private class CuratorClusterHook extends AbstractIdleService {
        @Override
        protected void startUp() throws Exception {
            hubCuratorCluster.addCacheListener();
            hubCuratorCluster.register();
        }

        @Override
        protected void shutDown() throws Exception {
            hubCuratorCluster.delete();
        }
    }
}

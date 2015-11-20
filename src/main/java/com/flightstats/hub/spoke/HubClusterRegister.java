package com.flightstats.hub.spoke;

import com.flightstats.hub.app.HubServices;
import com.flightstats.hub.cluster.CuratorCluster;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Inject;
import com.google.inject.name.Named;

public class HubClusterRegister {

    @Inject
    @Named("HubCuratorCluster")
    private CuratorCluster spokeCuratorCluster;

    public HubClusterRegister() {
        HubServices.register(new CuratorClusterHook(), HubServices.TYPE.FINAL_POST_START, HubServices.TYPE.PRE_STOP);
    }

    private class CuratorClusterHook extends AbstractIdleService {
        @Override
        protected void startUp() throws Exception {
            spokeCuratorCluster.addCacheListener();
            spokeCuratorCluster.register();
        }

        @Override
        protected void shutDown() throws Exception {
            spokeCuratorCluster.delete();
        }
    }
}

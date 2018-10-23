package com.flightstats.hub.spoke;

import com.flightstats.hub.app.HubServices;
import com.flightstats.hub.cluster.CuratorCluster;
import com.google.common.util.concurrent.AbstractIdleService;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * The SpokeClusterRegister uses ip addresses of each instance in the cluster.
 * It is created before the node is added to the load balancer.
 */
public class SpokeClusterRegister {

    private final CuratorCluster spokeCuratorCluster;

    @Inject
    public SpokeClusterRegister(@Named("SpokeCluster") CuratorCluster spokeCuratorCluster) {
        this.spokeCuratorCluster = spokeCuratorCluster;

        HubServices.register(new CuratorSpokeClusterHook(), HubServices.TYPE.PERFORM_HEALTH_CHECK, HubServices.TYPE.PRE_STOP);
    }

    private class CuratorSpokeClusterHook extends AbstractIdleService {
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

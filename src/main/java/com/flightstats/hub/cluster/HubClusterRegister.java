package com.flightstats.hub.cluster;

import com.flightstats.hub.app.HubServices;
import com.google.common.util.concurrent.AbstractIdleService;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

/**
 * The HubCuratorCluster uses the fully qualifed domain name of each host.
 * A Hub instance is added to this cluster after a healthy start.
 */
@Singleton
public class HubClusterRegister {

    private final CuratorCluster hubCuratorCluster;

    @Inject
    public HubClusterRegister(@Named("HubCluster") CuratorCluster hubCuratorCluster) {
        this.hubCuratorCluster = hubCuratorCluster;
        HubServices.register(new CuratorClusterHook(), HubServices.TYPE.AFTER_HEALTHY_START, HubServices.TYPE.PRE_STOP);
    }

    private class CuratorClusterHook extends AbstractIdleService {
        @Override
        protected void startUp() {
            hubCuratorCluster.addCacheListener();
            hubCuratorCluster.register();
        }

        @Override
        protected void shutDown() {
            hubCuratorCluster.delete();
        }
    }
}

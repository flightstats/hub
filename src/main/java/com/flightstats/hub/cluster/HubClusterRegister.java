package com.flightstats.hub.cluster;

import com.flightstats.hub.app.HubServices;
import com.google.common.util.concurrent.AbstractIdleService;
import javax.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

/**
 * The HubCuratorCluster uses the fully qualifed domain name of each host.
 * A Hub instance is added to this cluster after a healthy start.
 */
@Singleton
public class HubClusterRegister {

    @Inject
    @Named("HubCuratorCluster")
    private CuratorCluster hubCuratorCluster;

    public HubClusterRegister() {
        HubServices.register(new CuratorClusterHook(), HubServices.TYPE.AFTER_HEALTHY_START, HubServices.TYPE.PRE_STOP);
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

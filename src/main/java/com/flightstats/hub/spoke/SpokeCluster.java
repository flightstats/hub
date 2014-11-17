package com.flightstats.hub.spoke;

import com.google.inject.Inject;
import com.google.inject.name.Named;

//todo - gfm - 11/13/14 - this is dumb for now, would be better handle cluster state changes via ZooKeeper.
public class SpokeCluster {

    private final String[] servers;

    @Inject
    public SpokeCluster(@Named("spoke.servers") String spokeServers) {
        servers = spokeServers.split(",");
    }

    public String[] getServers() {
        return servers;
    }
}

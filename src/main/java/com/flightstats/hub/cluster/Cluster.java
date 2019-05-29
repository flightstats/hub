package com.flightstats.hub.cluster;

import java.util.List;
import java.util.Set;

public interface Cluster {

    /**
     * @return All servers in the cluster
     */
    Set<String> getAllServers();

    Set<String> getServers(String channel);

    List<String> getRemoteServers(String channel);

}

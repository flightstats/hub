package com.flightstats.hub.cluster;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface Cluster {
    /**
     * @return the localhost's server
     */
    static Collection<String> getLocalServer() {
        List<String> server = new ArrayList<>();
      //  server.add(Cluster.getHost(false));
        return server;
    }

    /*static String getHost(boolean useName) {
        if (useName) {
            return HubHost.getLocalNamePort();
        } else {
            return HubHost.getLocalAddressPort();
        }
    }*/

    /**
     * @return All servers in the cluster
     */
    Set<String> getAllServers();

    Set<String> getServers(String channel);

    default List<String> getRemoteServers(String channel) {
        List<String> servers = new ArrayList<>(getServers(channel));
        //servers.remove(getHost(true));
        return servers;
    }

}

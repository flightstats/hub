package com.flightstats.hub.cluster;

import com.flightstats.hub.app.HubHost;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface Cluster {
    /**
     * @return the localhost's server
     */
    Collection<String> getLocalServer() throws UnknownHostException;

    /**
     * @return All servers in the cluster
     */
    Set<String> getAllServers();

    Set<String> getServers(String channel);

    default List<String> getRemoteServers(String channel) {
        List<String> servers = new ArrayList<>(getServers(channel));
        servers.remove(getHost(true));
        return servers;
    }

    static String getHost(boolean useName) {
        if (useName) {
            return HubHost.getLocalNamePort();
        } else {
            return HubHost.getLocalAddressPort();
        }
    }

}

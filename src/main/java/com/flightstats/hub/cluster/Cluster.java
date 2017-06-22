package com.flightstats.hub.cluster;

import com.flightstats.hub.app.HubHost;

import java.net.UnknownHostException;
import java.util.*;

public interface Cluster extends Ring {
    /**
     * @return the localhost's server
     */
    Collection<String> getLocalServer() throws UnknownHostException;

    /**
     * @return All servers in the cluster
     */
    Set<String> getAllServers();

    default List<String> getRemoteServers(String channel) {
        List<String> servers = new ArrayList<>(getServers(channel));
        servers.remove(getHost(true));
        return servers;
    }

    default String getHost(boolean useName) {
        if (useName) {
            return HubHost.getLocalNamePort();
        } else {
            return HubHost.getLocalAddressPort();
        }
    }

    default List<String> randomize(Collection<String> incoming) {
        List<String> servers = new ArrayList<>(incoming);
        Collections.shuffle(servers);
        return servers;
    }
}

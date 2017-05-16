package com.flightstats.hub.cluster;

import com.flightstats.hub.app.HubHost;

import java.net.UnknownHostException;
import java.util.*;

public interface Cluster {
    /**
     * @return the localhost's server
     */
    Collection<String> getLocalServer() throws UnknownHostException;

    /**
     * @return All servers in the cluster
     */
    Set<String> getAllServers();

    /**
     * @return the current set of servers for this channel.
     */
    Set<String> getCurrentServers(String channel);

    //todo - gfm - these will need to know the time (range) of the request to determine the correct servers.

    default List<String> getRandomServers(String channel) {
        List<String> servers = new ArrayList<>(getCurrentServers(channel));
        Collections.shuffle(servers);
        return servers;
    }

    default List<String> getRandomRemoteServers(String channel) {
        List<String> servers = getRandomServers(channel);
        //todo - gfm - figure this out, can we remove the dichotomy between name and ip ???
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
}

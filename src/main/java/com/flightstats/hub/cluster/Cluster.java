package com.flightstats.hub.cluster;

import com.flightstats.hub.app.HubHost;

import java.net.UnknownHostException;
import java.util.*;

public interface Cluster {
    Collection<String> getLocalServer() throws UnknownHostException;

    Set<String> getAllServers();

    Set<String> getServers(String channel);

    default List<String> getRandomServers(String channel) {
        List<String> servers = new ArrayList<>(getServers(channel));
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

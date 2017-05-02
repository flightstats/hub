package com.flightstats.hub.cluster;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Set;

public class DynamicSpokeCluster implements Cluster {

    @Inject
    @Named("SpokeCuratorCluster")
    private Cluster spokeCluster;

    @Override
    public Collection<String> getLocalServer() throws UnknownHostException {
        return spokeCluster.getLocalServer();
    }

    @Override
    public Set<String> getAllServers() {
        return spokeCluster.getAllServers();
    }

    @Override
    public Set<String> getCurrentServers(String channel) {
        Set<String> servers = spokeCluster.getAllServers();
        if (servers.size() <= 3) {
            return servers;
        }



        /*


        server starts up
        reads historical ring information
           name
           event - add or remove
           start time (ctime or mtime)
           ip address

        creates historical and current rings
        adds own record to ZK

        For every change event
            ZK listener creates another cached ring
            Cached Ring
                Servers available
                    any calculations cached
                Start Applicable time
                    start time minus 1 seconds
                End Applicable Time
                    end time plus ???

        Scenarios:
            Get the current list of servers
            Get all available servers for a specific time
            Get all available servers for a time range

        Caveats:
            The ip address should come from the current server name in the ring

         */


        return null;
    }

    //CuratorCluster
}

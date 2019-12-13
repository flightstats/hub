package com.flightstats.hub.model;

import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.stream.Collectors;

@Value
@Builder
public class InternalCluster {
    ClusterObject object;

    public List<String> getWithinSpokeTtl() {
        return this.getObject()
                .getDecommissioned()
                .getWithinSpokeTTL()
                .stream()
                .map(ServerObject::getServer)
                .collect(Collectors.toList());
    }

    public List<String> getDoNotStartServers() {
        return this.getObject()
                .getDecommissioned()
                .getDoNotStart()
                .stream()
                .map(ServerObject::getServer)
                .collect(Collectors.toList());
    }
}

@Value
@Builder
class ClusterObject {
    Decommissioned decommissioned;
}

@Value
@Builder
class Decommissioned {
    List<ServerObject> withinSpokeTTL;
    List<ServerObject> doNotStart;
}


@Value
@Builder
class ServerObject {
    String server;
}

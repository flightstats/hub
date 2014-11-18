package com.flightstats.hub.spoke;

import java.util.Arrays;
import java.util.List;

public class StringSpokeCluster implements SpokeCluster {

    private final List<String> servers;

    public StringSpokeCluster(String spokeServers) {
        servers = Arrays.asList((String[]) spokeServers.split(","));
    }

    @Override
    public List<String> getServers() {
        return servers;
    }
}

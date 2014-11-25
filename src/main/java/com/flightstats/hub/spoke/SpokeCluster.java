package com.flightstats.hub.spoke;

import java.util.List;

public interface SpokeCluster {
    List<String> getServers();

    List<String> getRandomServers();

    public List<String> getOtherServers();
}

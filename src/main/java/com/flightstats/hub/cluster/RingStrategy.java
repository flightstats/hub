package com.flightstats.hub.cluster;

import java.util.List;
import java.util.Set;

public interface RingStrategy {

    Set<String> getServers(String channel);

    List<String> getAllServers();
}

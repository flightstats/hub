package com.flightstats.hub.cluster;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public interface DecommissionCluster {
    default List<String> filter(Set<String> servers) {
        return new ArrayList<>(servers);
    }
}

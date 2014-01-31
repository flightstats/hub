package com.flightstats.hub.replication;

import java.util.List;

public interface Replicator {
    List<DomainReplicator> getDomainReplicators();
}

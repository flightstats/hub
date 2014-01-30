package com.flightstats.datahub.replication;

import java.util.List;

public interface Replicator {
    List<DomainReplicator> getDomainReplicators();
}

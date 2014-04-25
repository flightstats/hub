package com.flightstats.hub.replication;

import com.google.common.base.Optional;

import java.util.Collection;

/**
 *
 */
public interface ReplicationService {
    void create(ReplicationDomain domain);

    Optional<ReplicationDomain> get(String domain);

    boolean delete(String domain);

    Collection<ReplicationDomain> getDomains(boolean refreshCache);

    ReplicationBean getReplicationBean();
}

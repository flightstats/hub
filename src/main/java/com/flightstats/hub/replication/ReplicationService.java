package com.flightstats.hub.replication;

import com.google.common.base.Optional;

import java.util.Collection;

/**
 *
 */
public interface ReplicationService {
    void create(String domain, ReplicationDomain config);

    Optional<ReplicationDomain> get(String domain);

    boolean delete(String domain);

    Collection<ReplicationDomain> getDomains();

    ReplicationBean getReplicationBean();
}

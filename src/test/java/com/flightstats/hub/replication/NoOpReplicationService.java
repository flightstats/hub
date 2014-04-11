package com.flightstats.hub.replication;

import com.google.common.base.Optional;

import java.util.Collection;
import java.util.Collections;

/**
 * NoOpReplicationService should only be used in testing.
 */
public class NoOpReplicationService implements ReplicationService {
    @Override
    public void create(String domain, ReplicationDomain config) { }

    @Override
    public Optional<ReplicationDomain> get(String domain) {
        return Optional.absent();
    }

    @Override
    public boolean delete(String domain) {
        return false;
    }

    @Override
    public Collection<ReplicationDomain> getDomains() {
        return Collections.emptyList();
    }

    @Override
    public ReplicationBean getReplicationBean() {
        return null;
    }
}

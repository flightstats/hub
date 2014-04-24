package com.flightstats.hub.replication;

import com.google.common.base.Optional;

import java.util.Collection;

public interface ReplicationDao {
    void upsert(ReplicationDomain config);

    Optional<ReplicationDomain> get(String domain);

    void delete(String domain);

    Collection<ReplicationDomain> getDomains(boolean refreshCache);
}

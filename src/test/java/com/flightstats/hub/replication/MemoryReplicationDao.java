package com.flightstats.hub.replication;

import com.google.common.base.Optional;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class MemoryReplicationDao implements ReplicationDao {
    private Map<String, ReplicationDomain> domainMap = new HashMap<>();

    @Override
    public void upsert(ReplicationDomain domain) {
        domainMap.put(domain.getDomain(), domain);
    }

    @Override
    public Optional<ReplicationDomain> get(String domain) {
        return Optional.fromNullable(domainMap.get(domain));
    }

    @Override
    public void delete(String domain) {
        domainMap.remove(domain);
    }

    @Override
    public Collection<ReplicationDomain> getDomains(boolean refreshCache) {
        return domainMap.values();
    }
}

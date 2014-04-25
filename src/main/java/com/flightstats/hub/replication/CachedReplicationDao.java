package com.flightstats.hub.replication;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import java.util.Collection;

/**
 *
 */
public class CachedReplicationDao implements ReplicationDao {

    public static final String DELEGATE = "ReplicationCacheDao.DELEGATE";
    private Collection<ReplicationDomain> domains;
    private final ReplicationDao replicationDao;

    @Inject
    public CachedReplicationDao(@Named(DELEGATE) ReplicationDao replicationDao) {
        this.replicationDao = replicationDao;
    }

    public synchronized Collection<ReplicationDomain> getDomains(boolean refreshCache) {
        if (domains == null || refreshCache) {
            domains = replicationDao.getDomains(refreshCache);
        }
        return domains;
    }

    @Override
    public void upsert(ReplicationDomain domain) {
        replicationDao.upsert(domain);
    }

    @Override
    public Optional<ReplicationDomain> get(String domain) {
        return replicationDao.get(domain);
    }

    @Override
    public void delete(String domain) {
        replicationDao.delete(domain);
    }
}

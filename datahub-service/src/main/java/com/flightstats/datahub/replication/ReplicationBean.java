package com.flightstats.datahub.replication;

import java.util.Collection;

/**
 *
 */
public class ReplicationBean {

    Collection<ReplicationDomain> domains;
    Collection<ReplicationStatus> status;

    public ReplicationBean(Collection<ReplicationDomain> domains, Collection<ReplicationStatus> status) {
        this.domains = domains;
        this.status = status;
    }

    public Collection<ReplicationDomain> getDomains() {
        return domains;
    }

    public Collection<ReplicationStatus> getStatus() {
        return status;
    }
}

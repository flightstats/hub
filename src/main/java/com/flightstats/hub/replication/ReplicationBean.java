package com.flightstats.hub.replication;

import java.util.Collection;
import java.util.Comparator;
import java.util.TreeSet;

/**
 *
 */
public class ReplicationBean {

    Collection<ReplicationDomain> domains;
    Collection<ReplicationStatus> status;

    public ReplicationBean(Collection<ReplicationDomain> domains, Collection<ReplicationStatus> status) {
        this.domains = new TreeSet<>(new Comparator<ReplicationDomain>() {

            @Override
            public int compare(ReplicationDomain rd1, ReplicationDomain rd2) {
                return rd1.getDomain().compareTo(rd2.getDomain());
            }
        });
        this.domains.addAll(domains);
        this.status = new TreeSet<>(new Comparator<ReplicationStatus>() {
            @Override
            public int compare(ReplicationStatus rs1, ReplicationStatus rs2) {
                return rs1.getUrl().compareTo(rs2.getUrl());
            }
        });
        this.status.addAll(status);
    }

    public Collection<ReplicationDomain> getDomains() {
        return domains;
    }

    public Collection<ReplicationStatus> getStatus() {
        return status;
    }
}

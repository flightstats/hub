package com.flightstats.datahub.replication;

/**
 *
 */
public class ReplicationStatus {

    private String url;
    private long replicationLatest;
    private long sourceLatest;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public long getReplicationLatest() {
        return replicationLatest;
    }

    public void setReplicationLatest(long replicationLatest) {
        this.replicationLatest = replicationLatest;
    }

    public long getSourceLatest() {
        return sourceLatest;
    }

    public void setSourceLatest(long sourceLatest) {
        this.sourceLatest = sourceLatest;
    }

    public long getDeltaLatest() {
        return getSourceLatest() - getReplicationLatest();
    }
}

package com.flightstats.datahub.replication;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 *
 */
public class ReplicationStatus {

    private String url;
    private long replicationLatest;
    private long sourceLatest;
    //todo - gfm - 1/27/14 - add dates?

    public String getUrl() {
        return url;
    }

    @JsonProperty("sourceUrl")
    public void setUrl(String url) {
        this.url = url;
    }

    @JsonProperty("replicationLatest")
    public long getReplicationLatest() {
        return replicationLatest;
    }

    public void setReplicationLatest(long replicationLatest) {
        this.replicationLatest = replicationLatest;
    }

    @JsonProperty("sourceLatest")
    public long getSourceLatest() {
        return sourceLatest;
    }

    public void setSourceLatest(long sourceLatest) {
        this.sourceLatest = sourceLatest;
    }
}

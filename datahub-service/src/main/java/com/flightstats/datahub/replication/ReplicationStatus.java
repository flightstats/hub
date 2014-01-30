package com.flightstats.datahub.replication;

/**
 *
 */
public class ReplicationStatus {

    private long replicationLatest;
    private long sourceLatest;
    private Channel channel;

    public String getUrl() {
        return channel.getUrl();
    }

    public String getName() {
        return channel.getName();
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
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

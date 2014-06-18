package com.flightstats.hub.replication;

public class ReplicationStatus {

    private long replicationLatest;
    private long sourceLatest;
    private Channel channel;
    private boolean connected;
    private String message;

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

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

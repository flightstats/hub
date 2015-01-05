package com.flightstats.hub.replication;

public class ReplicationStatus {

    private String replicationLatest;
    private String sourceLatest;
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

    public String getReplicationLatest() {
        return replicationLatest;
    }

    public void setReplicationLatest(String replicationLatest) {
        this.replicationLatest = replicationLatest;
    }

    public String getSourceLatest() {
        return sourceLatest;
    }

    public void setSourceLatest(String sourceLatest) {
        this.sourceLatest = sourceLatest;
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

package com.flightstats.hub.replication;

import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.model.ChannelConfiguration;
import com.flightstats.hub.util.HubUtils;

public class V2ChannelReplicator implements ChannelReplicator {

    private ChannelConfiguration channel;
    private HubUtils hubUtils;
    private final String appUrl = HubProperties.getProperty("app.url", "");

    public V2ChannelReplicator(ChannelConfiguration channel, HubUtils hubUtils) {
        this.channel = channel;
        this.hubUtils = hubUtils;
    }

    public void start() {
        hubUtils.startGroupCallback(getGroupName(), getCallbackUrl(), channel.getReplicationSource());
    }

    private String getCallbackUrl() {
        return appUrl + "internal/replication/" + channel.getName();
    }

    private String getGroupName() {
        return "Replication_" + channel.getName();
    }

    @Override
    public ChannelConfiguration getChannel() {
        return null;
    }

    @Override
    public void stop() {
        hubUtils.stopGroupCallback(getGroupName(), channel.getReplicationSource());
    }

    @Override
    public void exit() {
        //do nothing
    }
}

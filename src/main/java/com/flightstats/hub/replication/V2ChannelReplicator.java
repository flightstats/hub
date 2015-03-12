package com.flightstats.hub.replication;

import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.model.ChannelConfiguration;
import com.flightstats.hub.util.HubUtils;

public class V2ChannelReplicator implements ChannelReplicator {

    private ChannelConfiguration channel;
    private HubUtils hubUtils;
    private final String appUrl = HubProperties.getProperty("app.url", "");
    private final String appEnv;

    public V2ChannelReplicator(ChannelConfiguration channel, HubUtils hubUtils) {
        this.channel = channel;
        this.hubUtils = hubUtils;
        appEnv = (HubProperties.getProperty("app.name", "hub")
                + "_" + HubProperties.getProperty("app.environment", "unknown")).replace("-", "_");
    }

    public void start() {
        hubUtils.startGroupCallback(getGroupName(), getCallbackUrl(), channel.getReplicationSource());
    }

    private String getCallbackUrl() {
        return appUrl + "internal/replication/" + channel.getName();
    }

    private String getGroupName() {
        return "Repl_" + appEnv + "_" + channel.getName();
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

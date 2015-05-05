package com.flightstats.hub.replication;

import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.util.HubUtils;
import org.apache.commons.lang3.StringUtils;

public class V2ChannelReplicator implements ChannelReplicator {

    private ChannelConfig channel;
    private HubUtils hubUtils;
    private final String appUrl = StringUtils.appendIfMissing(HubProperties.getProperty("app.url", ""), "/");
    private final String appEnv;

    public V2ChannelReplicator(ChannelConfig channel, HubUtils hubUtils) {
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
    public ChannelConfig getChannel() {
        return channel;
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

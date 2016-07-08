package com.flightstats.hub.replication;

import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.app.HubProvider;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.util.HubUtils;
import com.flightstats.hub.webhook.Group;

class ChannelReplicator implements Replicator {

    private static final HubUtils hubUtils = HubProvider.getInstance(HubUtils.class);

    private ChannelConfig channel;

    ChannelReplicator(ChannelConfig channel) {
        this.channel = channel;
    }

    public void start() {
        Group.GroupBuilder builder = Group.builder()
                .name(getGroupName())
                .callbackUrl(getCallbackUrl())
                .channelUrl(channel.getReplicationSource())
                .heartbeat(true)
                .batch(Group.SECOND);
        hubUtils.startGroupCallback(builder.build());
    }

    private String getCallbackUrl() {
        return HubProperties.getAppUrl() + "internal/repls/" + channel.getName();
    }

    private String getGroupName() {
        return "Repl_" + HubProperties.getAppEnv() + "_" + channel.getName();
    }

    public ChannelConfig getChannel() {
        return channel;
    }

    public void stop() {
        hubUtils.stopGroupCallback(getGroupName(), channel.getReplicationSource());
    }

}

package com.flightstats.hub.replication;

import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.group.Group;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.util.HubUtils;

public class ChannelReplicator {

    public static final String REPLICATED_LAST_UPDATED = "/ReplicatedLastUpdated/";
    private ChannelConfig channel;
    private HubUtils hubUtils;

    ChannelReplicator(ChannelConfig channel, HubUtils hubUtils) {
        this.channel = channel;
        this.hubUtils = hubUtils;
    }

    public void start() {
        Group.GroupBuilder builder = Group.builder()
                .name(getGroupName())
                .callbackUrl(getCallbackUrl("repls"))
                .channelUrl(channel.getReplicationSource())
                .heartbeat(true)
                .batch(Group.SECOND);
        hubUtils.startGroupCallback(builder.build());
    }

    private String getCallbackUrl(String apiPath) {
        return HubProperties.getAppUrl() + "internal/" + apiPath + "/" + channel.getName();
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

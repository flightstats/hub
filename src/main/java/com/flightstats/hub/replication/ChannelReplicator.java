package com.flightstats.hub.replication;

import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.group.Group;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.util.HubUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChannelReplicator {

    private final static Logger logger = LoggerFactory.getLogger(ChannelReplicator.class);

    private ChannelConfig channel;
    private HubUtils hubUtils;

    public static final String REPLICATED_LAST_UPDATED = "/ReplicatedLastUpdated/";

    public ChannelReplicator(ChannelConfig channel, HubUtils hubUtils) {
        this.channel = channel;
        this.hubUtils = hubUtils;
    }

    public void start() {
        Group.GroupBuilder builder = Group.builder()
                .name(getGroupName())
                .callbackUrl(getCallbackUrl())
                .channelUrl(channel.getReplicationSource())
                .heartbeat(true)
                .batch(Group.SINGLE);
        Group group = builder.build();
        hubUtils.startGroupCallback(group);
    }

    private String getCallbackUrl() {
        return HubProperties.getAppUrl() + "internal/replication/" + channel.getName();
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

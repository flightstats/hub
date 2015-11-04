package com.flightstats.hub.replication;

import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.group.Group;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.util.HubUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class S3Batch {

    private final static Logger logger = LoggerFactory.getLogger(S3Batch.class);

    private ChannelConfig channel;
    private HubUtils hubUtils;

    public S3Batch(ChannelConfig channel, HubUtils hubUtils) {
        this.channel = channel;
        this.hubUtils = hubUtils;
    }

    public void start() {
        Group.GroupBuilder builder = Group.builder()
                .name(getGroupName())
                .callbackUrl(getCallbackUrl())
                .channelUrl(getChannelUrl())
                .heartbeat(true)
                .batch(Group.MINUTE);
        Group group = builder.build();
        hubUtils.startGroupCallback(group);
    }

    private String getChannelUrl() {
        return HubProperties.getAppUrl() + "channel/" + channel.getName();
    }

    private String getCallbackUrl() {
        return HubProperties.getAppUrl() + "internal/s3Batch/" + channel.getName();
    }

    private String getGroupName() {
        return "S3Batch_" + HubProperties.getAppEnv() + "_" + channel.getName();
    }

    public ChannelConfig getChannel() {
        return channel;
    }

    public void stop() {
        hubUtils.stopGroupCallback(getGroupName(), getChannelUrl());
    }

}

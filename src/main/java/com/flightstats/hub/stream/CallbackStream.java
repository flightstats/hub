package com.flightstats.hub.stream;

import com.flightstats.hub.app.HubHost;
import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.group.Group;
import com.flightstats.hub.util.HubUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.glassfish.jersey.media.sse.EventOutput;

public class CallbackStream {
    private String channel;
    private EventOutput eventOutput;
    private HubUtils hubUtils;
    private final String random = RandomStringUtils.randomAlphanumeric(6);

    public CallbackStream(String channel, EventOutput eventOutput, HubUtils hubUtils) {
        this.channel = channel;
        this.eventOutput = eventOutput;
        this.hubUtils = hubUtils;
    }

    public void start() {
        Group.GroupBuilder builder = Group.builder()
                .name(getGroupName())
                .callbackUrl(getCallbackUrl())
                .channelUrl(getChannelUrl())
                .heartbeat(false)
                .batch(Group.SINGLE);
        Group group = builder.build();
        hubUtils.startGroupCallback(group);
    }

    public EventOutput getEventOutput() {
        return eventOutput;
    }

    private String getChannelUrl() {
        return HubProperties.getAppUrl() + "channel/" + channel;
    }

    private String getCallbackUrl() {
        return HubHost.getLocalHttpIpUri() + "internal/stream/" + getGroupName();
    }

    public String getGroupName() {
        return "Stream_" + HubProperties.getAppEnv() + "_" + channel + "_" + random;
    }

    public void stop() {
        IOUtils.closeQuietly(eventOutput);
        hubUtils.stopGroupCallback(getGroupName(), getChannelUrl());
    }

}

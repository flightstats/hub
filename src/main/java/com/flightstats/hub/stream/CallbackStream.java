package com.flightstats.hub.stream;

import com.flightstats.hub.app.HubHost;
import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.group.Group;
import com.flightstats.hub.util.HubUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;

public class CallbackStream {

    private ContentOutput contentOutput;
    private HubUtils hubUtils;
    private final String random = RandomStringUtils.randomAlphanumeric(6);

    public CallbackStream(ContentOutput contentOutput, HubUtils hubUtils) {
        this.contentOutput = contentOutput;
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

    public ContentOutput getContentOutput() {
        return contentOutput;
    }

    private String getChannelUrl() {
        return HubProperties.getAppUrl() + "channel/" + contentOutput.getChannel();
    }

    private String getCallbackUrl() {
        return HubHost.getLocalHttpIpUri() + "/internal/stream/" + getGroupName();
    }

    public String getGroupName() {
        return "Stream_" + HubProperties.getAppEnv() + "_" + contentOutput.getChannel() + "_" + random;
    }

    public void stop() {
        IOUtils.closeQuietly(contentOutput);
        hubUtils.stopGroupCallback(getGroupName(), getChannelUrl());
    }

}

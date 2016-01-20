package com.flightstats.hub.stream;

import com.flightstats.hub.app.HubHost;
import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.app.HubProvider;
import com.flightstats.hub.group.Group;
import com.flightstats.hub.group.GroupService;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;

public class CallbackStream {

    private ContentOutput contentOutput;
    private final GroupService groupService = HubProvider.getInstance(GroupService.class);
    private final String random = RandomStringUtils.randomAlphanumeric(6);

    public CallbackStream(ContentOutput contentOutput) {
        this.contentOutput = contentOutput;
    }

    public void start() {
        Group.GroupBuilder builder = Group.builder()
                .name(getGroupName())
                .callbackUrl(getCallbackUrl())
                .channelUrl(getChannelUrl())
                .heartbeat(true)
                .startingKey(contentOutput.getContentKey())
                .batch(Group.SINGLE);
        Group group = builder.build();
        groupService.upsertGroup(group);
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
        groupService.delete(getGroupName());
    }

}

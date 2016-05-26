package com.flightstats.hub.replication;

import com.flightstats.hub.app.HubProvider;
import com.flightstats.hub.group.Group;
import com.flightstats.hub.group.GroupService;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.util.HubUtils;

class GlobalReplicator implements Replicator {

    private static final HubUtils hubUtils = HubProvider.getInstance(HubUtils.class);
    private static final GroupService groupService = HubProvider.getInstance(GroupService.class);

    private final String satellite;
    private final ChannelConfig channel;

    GlobalReplicator(ChannelConfig channel, String satellite) {
        this.channel = channel;
        this.satellite = satellite;
    }

    public void start() {
        String channelName = channel.getName();
        hubUtils.putChannel(satellite + "internal/global/satellite/" + channelName, channel);
        Group group = Group.builder()
                .name(getGroupName())
                .callbackUrl(satellite + "internal/repls/" + channelName)
                .channelUrl(channel.getGlobal().getMaster() + "channel/" + channelName)
                .heartbeat(true)
                .batch(Group.SECOND)
                .build();
        groupService.upsertGroup(group);
    }

    private String getGroupName() {
        return "Global_" + getKey();
    }

    public ChannelConfig getChannel() {
        return channel;
    }

    public void stop() {
        groupService.delete(getGroupName());
    }

    public String getKey() {
        return satellite + "_" + channel.getName();
    }

}

package com.flightstats.hub.replication;

import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.app.HubProvider;
import com.flightstats.hub.group.GroupService;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.util.HubUtils;

class GlobalReplicator {

    private static final HubUtils hubUtils = HubProvider.getInstance(HubUtils.class);
    private static final GroupService groupService = HubProvider.getInstance(GroupService.class);
    private final String satellite;
    private ChannelConfig channel;

    GlobalReplicator(ChannelConfig channel, String satellite) {
        this.channel = channel;
        this.satellite = satellite;
    }

    public void start() {
        //todo - gfm - 5/25/16 - create channel
        //todo - gfm - 5/25/16 - start local Group using GroupService
        /*Group.GroupBuilder builder = Group.builder()
                .name(getGroupName())
                .callbackUrl(getCallbackUrl("repls"))
                .channelUrl(channel.getReplicationSource())
                .heartbeat(true)
                .batch(Group.SECOND);
        hubUtils.startGroupCallback(builder.build());*/
    }

    private String getCallbackUrl(String apiPath) {
        return HubProperties.getAppUrl() + "internal/repls/" + channel.getName();
    }

    private String getGroupName() {
        //todo - gfm - 5/25/16 - change this
        return "GLOBAL_" + HubProperties.getAppEnv() + "_" + channel.getName();
    }

    public ChannelConfig getChannel() {
        return channel;
    }

    public void stop() {
        //todo - gfm - 5/25/16 - stop
    }

    //todo - gfm - 5/25/16 - master-satellite-channel

}

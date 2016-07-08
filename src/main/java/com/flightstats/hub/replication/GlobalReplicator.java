package com.flightstats.hub.replication;

import com.flightstats.hub.app.HubProvider;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.util.HubUtils;
import com.flightstats.hub.webhook.Group;
import com.flightstats.hub.webhook.GroupService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class GlobalReplicator implements Replicator {

    private final static Logger logger = LoggerFactory.getLogger(GlobalReplicator.class);

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
        try {
            logger.info("starting global replication {}", channel);
            hubUtils.putChannel(satellite + "internal/global/satellite/" + channelName, channel);
            String groupName = getGroupName();
            logger.info("put channel {} {}", channel, groupName);
            Group group = Group.builder()
                    .name(groupName)
                    .callbackUrl(satellite + "internal/global/repl/" + channelName)
                    .channelUrl(channel.getGlobal().getMaster() + "channel/" + channelName)
                    .heartbeat(true)
                    .batch(Group.SECOND)
                    .build();
            groupService.upsertGroup(group);
            logger.info("upserted group {} {}", channel, group);
        } catch (Exception e) {
            logger.warn("unable to start " + channelName + " " + getGroupName(), e);
        }
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

    String getKey() {
        String domain = StringUtils.removeEnd(StringUtils.substringAfter(satellite, "://"), "/");
        return StringUtils.replace(StringUtils.replace(domain, ":", "_"), ".", "_") + "_" + channel.getName();
    }

}

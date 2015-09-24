package com.flightstats.hub.replication;

import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.group.Group;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.util.HubUtils;
import com.google.common.base.Optional;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChannelReplicatorImpl implements ChannelReplicator {

    private final static Logger logger = LoggerFactory.getLogger(ChannelReplicatorImpl.class);

    private ChannelConfig channel;
    private HubUtils hubUtils;
    private final String appUrl = StringUtils.appendIfMissing(HubProperties.getProperty("app.url", ""), "/");
    private final String appEnv;
    public static final String REPLICATED_LAST_UPDATED = "/ReplicatedLastUpdated/";

    public ChannelReplicatorImpl(ChannelConfig channel, HubUtils hubUtils) {
        this.channel = channel;
        this.hubUtils = hubUtils;
        appEnv = (HubProperties.getProperty("app.name", "hub")
                + "_" + HubProperties.getProperty("app.environment", "unknown")).replace("-", "_");
    }

    public void start() {
        //todo - gfm - 9/24/15 - for now, we need to roll this out to all envs using SINGLE replication
        //todo - gfm - 9/24/15 - after this is n all envs, we can change to MINUTE replication
        Optional<Group> groupOptional = hubUtils.getGroupCallback(getGroupName(), channel.getReplicationSource());
        Group.GroupBuilder builder = Group.builder()
                .name(getGroupName())
                .callbackUrl(getCallbackUrl())
                .channelUrl(channel.getReplicationSource())
                .batch(Group.SINGLE);
        if (groupOptional.isPresent()) {
            Group group = groupOptional.get();
            if (group.isMinute()) {
                logger.info("stopping MINUTE group {}", group);
                hubUtils.stopGroupCallback(getGroupName(), channel.getReplicationSource());
                builder.startingKey(group.getStartingKey());
            }
        }
        Group group = builder.build();
        hubUtils.startGroupCallback(group);
    }

    private String getCallbackUrl() {
        return appUrl + "internal/replication/" + channel.getName();
    }

    private String getGroupName() {
        return "Repl_" + appEnv + "_" + channel.getName();
    }

    @Override
    public ChannelConfig getChannel() {
        return channel;
    }

    @Override
    public void stop() {
        hubUtils.stopGroupCallback(getGroupName(), channel.getReplicationSource());
    }

    @Override
    public void exit() {
        //do nothing
    }
}

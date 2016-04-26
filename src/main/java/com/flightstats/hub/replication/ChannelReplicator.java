package com.flightstats.hub.replication;

import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.group.Group;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.util.HubUtils;
import com.sun.jersey.api.client.ClientResponse;
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
        ClientResponse response = getClientResponse("repls", Group.SECOND);
        if (response.getStatus() >= 400) {
            response = getClientResponse("replication", Group.SINGLE);
        }
    }

    private ClientResponse getClientResponse(String apiPath, String batch) {
        Group.GroupBuilder builder = Group.builder()
                .name(getGroupName())
                .callbackUrl(getCallbackUrl(apiPath))
                .channelUrl(channel.getReplicationSource())
                .heartbeat(true)
                .batch(batch);
        return hubUtils.startGroupCallback(builder.build());
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

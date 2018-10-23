package com.flightstats.hub.replication;

import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.util.HubUtils;
import com.flightstats.hub.webhook.Webhook;

class ChannelReplicator implements Replicator {

    private final HubUtils hubUtils;
    private final HubProperties hubProperties;
    private final ChannelConfig channel;

    ChannelReplicator(ChannelConfig channel, HubUtils hubUtils, HubProperties hubProperties) {
        this.channel = channel;
        this.hubUtils = hubUtils;
        this.hubProperties = hubProperties;
    }

    public void start() {
        Webhook.WebhookBuilder builder = Webhook.builder()
                .name(getGroupName())
                .callbackUrl(getCallbackUrl())
                .channelUrl(channel.getReplicationSource())
                .heartbeat(true)
                .callbackTimeoutSeconds(5 * 60)
                .batch(Webhook.SECOND);
        hubUtils.startWebhook(builder.build());
    }

    private String getCallbackUrl() {
        return hubProperties.getAppUrl() + "internal/repls/" + channel.getDisplayName();
    }

    private String getGroupName() {
        return "Repl_" + hubProperties.getAppEnv() + "_" + channel.getDisplayName();
    }

    public ChannelConfig getChannel() {
        return channel;
    }

    public void stop() {
        hubUtils.stopGroupCallback(getGroupName(), channel.getReplicationSource());
    }

}

package com.flightstats.hub.replication;

import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.app.HubProvider;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.util.HubUtils;
import com.flightstats.hub.webhook.Webhook;

class ChannelReplicator implements Replicator {

    private static final HubUtils hubUtils = HubProvider.getInstance(HubUtils.class);

    private ChannelConfig channel;

    ChannelReplicator(ChannelConfig channel) {
        this.channel = channel;
    }

    public void start() {
        Webhook.WebhookBuilder builder = Webhook.builder()
                .name(getGroupName())
                .callbackUrl(getCallbackUrl())
                .channelUrl(channel.getReplicationSource())
                .heartbeat(true)
                .batch(Webhook.SECOND);
        hubUtils.startWebhook(builder.build());
    }

    private String getCallbackUrl() {
        return HubProperties.getAppUrl() + "internal/repls/" + channel.getDisplayName();
    }

    private String getGroupName() {
        return "Repl_" + HubProperties.getAppEnv() + "_" + channel.getDisplayName();
    }

    public ChannelConfig getChannel() {
        return channel;
    }

    public void stop() {
        hubUtils.stopGroupCallback(getGroupName(), channel.getReplicationSource());
    }

}

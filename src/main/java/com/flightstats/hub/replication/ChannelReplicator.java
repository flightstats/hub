package com.flightstats.hub.replication;

import com.flightstats.hub.app.HubProvider;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.util.HubUtils;
import com.flightstats.hub.webhook.Webhook;

class ChannelReplicator implements Replicator {

    private static final HubUtils hubUtils = HubProvider.getInstance(HubUtils.class);

    private ChannelConfig channel;
    private String appUrl;
    private String appEnv;


    ChannelReplicator(ChannelConfig channel,
                      String appUrl,
                      String appEnv) {
        this.channel = channel;
        this.appUrl = appUrl;
        this.appEnv = appEnv;
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
        return appUrl + "internal/repls/" + channel.getDisplayName();
    }

    private String getGroupName() {
        return "Repl_" + appEnv + "_" + channel.getDisplayName();
    }

    public ChannelConfig getChannel() {
        return channel;
    }

    public void stop() {
        hubUtils.stopGroupCallback(getGroupName(), channel.getReplicationSource());
    }

}

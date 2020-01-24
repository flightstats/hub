package com.flightstats.hub.replication;

import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.util.HubUtils;
import com.flightstats.hub.webhook.Webhook;

import static com.flightstats.hub.model.WebhookType.SECOND;

class ChannelReplicator implements Replicator {

    private final HubUtils hubUtils;
    private final ChannelConfig channel;
    private final String appUrl;
    private final String appEnv;


    ChannelReplicator(HubUtils hubUtils,
                      ChannelConfig channel,
                      String appUrl,
                      String appEnv) {
        this.hubUtils = hubUtils;
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
                .batch(SECOND.name());
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
        hubUtils.deleteWebhook(getGroupName(), channel.getReplicationSource());
    }

}

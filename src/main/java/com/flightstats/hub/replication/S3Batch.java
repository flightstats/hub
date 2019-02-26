package com.flightstats.hub.replication;

import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.util.HubUtils;
import com.flightstats.hub.webhook.Webhook;
import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.lang3.StringUtils;

public class S3Batch {

    @VisibleForTesting
    static final String S3_BATCH = "S3Batch_";
    private static final int TTL_MINUTES = 60 * 6;
    private static final int MAX_ATTEMPTS = 12;
    private ChannelConfig channel;
    private HubUtils hubUtils;

    public S3Batch(ChannelConfig channel, HubUtils hubUtils) {
        this.channel = channel;
        this.hubUtils = hubUtils;
    }

    public static boolean isS3BatchCallback(String groupName) {
        return StringUtils.startsWith(groupName, S3_BATCH);
    }

    public void start() {
        Webhook webhook = buildS3BatchWebhook();
        hubUtils.startWebhook(webhook);
    }

    @VisibleForTesting
    Webhook buildS3BatchWebhook() {
        return Webhook.builder()
                .name(getGroupName())
                .callbackUrl(getCallbackUrl())
                .channelUrl(getChannelUrl())
                .heartbeat(true)
                .parallelCalls(2)
                .ttlMinutes(TTL_MINUTES)
                .maxAttempts(MAX_ATTEMPTS)
                .batch(Webhook.MINUTE)
                .build();
    }

    private String getChannelUrl() {
        return HubProperties.getAppUrl() + "channel/" + channel.getDisplayName();
    }

    private String getCallbackUrl() {
        return HubProperties.getAppUrl() + "internal/s3Batch/" + channel.getDisplayName();
    }

    public String getGroupName() {
        return S3_BATCH + HubProperties.getAppEnv() + "_" + channel.getDisplayName();
    }

    public ChannelConfig getChannel() {
        return channel;
    }

    public void stop() {
        hubUtils.stopGroupCallback(getGroupName(), getChannelUrl());
    }

}

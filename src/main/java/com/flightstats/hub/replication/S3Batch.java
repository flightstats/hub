package com.flightstats.hub.replication;

import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.util.HubUtils;
import com.flightstats.hub.webhook.Webhook;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class S3Batch {

    private final static Logger logger = LoggerFactory.getLogger(S3Batch.class);
    private static final String S3_BATCH = "S3Batch_";

    private ChannelConfig channel;
    private HubUtils hubUtils;

    public S3Batch(ChannelConfig channel, HubUtils hubUtils) {
        this.channel = channel;
        this.hubUtils = hubUtils;
    }

    public void start() {
        Webhook.WebhookBuilder builder = Webhook.builder()
                .name(getGroupName())
                .callbackUrl(getCallbackUrl())
                .channelUrl(getChannelUrl())
                .heartbeat(true)
                .parallelCalls(2)
                .batch(Webhook.MINUTE);
        Webhook webhook = builder.build();
        hubUtils.startWebhook(webhook);
    }

    private String getChannelUrl() {
        return HubProperties.getAppUrl() + "channel/" + channel.getName();
    }

    private String getCallbackUrl() {
        return HubProperties.getAppUrl() + "internal/s3Batch/" + channel.getName();
    }

    public String getGroupName() {
        return S3_BATCH + HubProperties.getAppEnv() + "_" + channel.getName();
    }

    public ChannelConfig getChannel() {
        return channel;
    }

    public void stop() {
        hubUtils.stopGroupCallback(getGroupName(), getChannelUrl());
    }

    public static boolean isS3BatchCallback(String groupName) {
        return StringUtils.startsWith(groupName, S3_BATCH);
    }

}

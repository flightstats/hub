package com.flightstats.hub.replication;

import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.util.HubUtils;
import com.flightstats.hub.webhook.Webhook;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class S3BatchTest {
    @Test
    void testBatchWebhookCreation() {
        final String appUrl = "http://localhost:9080/";
        final String appEnv = "hub-dev";
        HubUtils hubUtils = mock(HubUtils.class);
        ChannelConfig config = ChannelConfig.builder().name("test").build();
        S3Batch batch = new S3Batch(config, hubUtils, appUrl, appEnv);

        Webhook webhook = batch.buildS3BatchWebhook();

        assertEquals(S3Batch.S3_BATCH + appEnv + "_test", webhook.getName());
        assertEquals(appUrl + "internal/s3Batch/test", webhook.getCallbackUrl());
        assertEquals(appUrl + "channel/test", webhook.getChannelUrl());
        assertTrue(webhook.isHeartbeat());
        assertEquals((Integer) 2, webhook.getParallelCalls());
        assertEquals((Integer) 360, webhook.getTtlMinutes());
        assertEquals((Integer) 12, webhook.getMaxAttempts());
        assertEquals("MINUTE", webhook.getBatch());
    }

}
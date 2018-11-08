package com.flightstats.hub.replication;

import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.util.HubUtils;
import com.flightstats.hub.app.HubBindings;
import com.flightstats.hub.webhook.Webhook;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.*;

public class S3BatchTest {
    private static final Logger logger = LoggerFactory.getLogger(S3BatchTest.class);

    @Test
    public void testBatchWebhookCreation() {
        ChannelConfig config = ChannelConfig.builder().name("defaults").build();
        HubUtils hubUtils = new HubUtils(HubBindings.buildJerseyClientNoRedirects(), HubBindings.buildJerseyClient());
        S3Batch batch = new S3Batch(config, hubUtils);
        String name = batch.getGroupName();
        assertTrue(name.contains("S3Batch_hub_"));
    }

    @Test
    public void testBatchWebhookCreationConfig() {
        ChannelConfig config = ChannelConfig.builder().name("defaults").build();
        HubUtils hubUtils = new HubUtils(HubBindings.buildJerseyClientNoRedirects(), HubBindings.buildJerseyClient());
        S3Batch batch = new S3Batch(config, hubUtils);
        batch.start(true);
        Webhook webhook = batch.getWebhook();
        assertEquals(webhook.getBatch(), "MINUTE");
        long ttlMins =  webhook.getTtlMinutes();
        assertEquals(ttlMins, 360);
        int maxAttempts = webhook.getMaxAttempts();
        assertEquals(maxAttempts,12);
    }

}

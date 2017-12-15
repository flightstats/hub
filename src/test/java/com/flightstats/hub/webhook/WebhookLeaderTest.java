package com.flightstats.hub.webhook;

import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.util.TimeUtil;
import org.joda.time.DateTime;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WebhookLeaderTest {

    @Test()
    public void testWebhookOK() {
        DateTime twentyMinutesAgo = TimeUtil.now().minusMinutes(20);
        ContentKey contentKey = new ContentKey(twentyMinutesAgo);
        Webhook webhook = Webhook.builder().ttlMinutes(21).build().withDefaults();
        ChannelConfig channelConfig = ChannelConfig.builder().ttlDays(10).build();
        WebhookStatus status = mock(WebhookStatus.class);
        when(status.getErrors()).thenReturn(new ArrayList<>());
        WebhookLeader.checkExpiration(contentKey, channelConfig, webhook, status);
    }

    @Test(expected = ItemExpiredException.class)
    public void testWebhookExpired() {
        DateTime twentyMinutesAgo = TimeUtil.now().minusMinutes(20);
        ContentKey contentKey = new ContentKey(twentyMinutesAgo);
        Webhook webhook = Webhook.builder().ttlMinutes(19).build().withDefaults();
        WebhookStatus status = mock(WebhookStatus.class);
        when(status.getErrors()).thenReturn(new ArrayList<>());
        WebhookLeader.checkExpiration(contentKey, null, webhook, status);
    }

    @Test
    public void testChannelExpiration() {
        DateTime tenDaysAgo = TimeUtil.now().minusDays(10).plusMinutes(1);
        ContentKey contentKey = new ContentKey(tenDaysAgo);
        Webhook webhook = Webhook.builder().ttlMinutes(0).build().withDefaults();
        ChannelConfig channelConfig = ChannelConfig.builder().ttlDays(10).build();
        WebhookStatus status = mock(WebhookStatus.class);
        when(status.getErrors()).thenReturn(new ArrayList<>());
        WebhookLeader.checkExpiration(contentKey, channelConfig, webhook, status);
    }

    @Test(expected = ItemExpiredException.class)
    public void testChannelExpired() {
        DateTime tenDaysAgo = TimeUtil.now().minusDays(10).minusMinutes(1);
        ContentKey contentKey = new ContentKey(tenDaysAgo);
        Webhook webhook = Webhook.builder().ttlMinutes(0).build().withDefaults();
        ChannelConfig channelConfig = ChannelConfig.builder().ttlDays(10).build();
        WebhookStatus status = mock(WebhookStatus.class);
        when(status.getErrors()).thenReturn(new ArrayList<>());
        WebhookLeader.checkExpiration(contentKey, channelConfig, webhook, status);
    }

    @Test(expected = ItemExpiredException.class)
    public void testMaxAttemptsReached() {
        ContentKey contentKey = new ContentKey(TimeUtil.now());
        Webhook webhook = Webhook.builder().maxAttempts(1).build().withDefaults();
        ChannelConfig channelConfig = ChannelConfig.builder().build();
        WebhookStatus status = mock(WebhookStatus.class);
        List<String> errors = Collections.singletonList(contentKey.toUrl());
        when(status.getErrors()).thenReturn(errors);
        WebhookLeader.checkExpiration(contentKey, channelConfig, webhook, status);
    }

}
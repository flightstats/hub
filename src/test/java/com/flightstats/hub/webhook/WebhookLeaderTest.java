package com.flightstats.hub.webhook;

import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.util.TimeUtil;
import org.joda.time.DateTime;
import org.junit.Test;

public class WebhookLeaderTest {

    @Test()
    public void testWebhookOK() {
        DateTime twentyMinutesAgo = TimeUtil.now().minusMinutes(20);
        ContentKey contentKey = new ContentKey(twentyMinutesAgo);
        Webhook webhook = Webhook.builder().ttlMinutes(21).build();
        ChannelConfig channelConfig = ChannelConfig.builder().ttlDays(10).build();
        WebhookLeader.checkExpiration(contentKey, channelConfig, webhook);
    }

    @Test(expected = ItemExpiredException.class)
    public void testWebhookExpired() {
        DateTime twentyMinutesAgo = TimeUtil.now().minusMinutes(20);
        ContentKey contentKey = new ContentKey(twentyMinutesAgo);
        Webhook webhook = Webhook.builder().ttlMinutes(19).build();
        WebhookLeader.checkExpiration(contentKey, null, webhook);
    }

    @Test
    public void testChannelExpiration() {
        DateTime tenDaysAgo = TimeUtil.now().minusDays(10).plusMinutes(1);
        ContentKey contentKey = new ContentKey(tenDaysAgo);
        Webhook webhook = Webhook.builder().ttlMinutes(0).build();
        ChannelConfig channelConfig = ChannelConfig.builder().ttlDays(10).build();
        WebhookLeader.checkExpiration(contentKey, channelConfig, webhook);
    }

    @Test(expected = ItemExpiredException.class)
    public void testChannelExpired() {
        DateTime tenDaysAgo = TimeUtil.now().minusDays(10).minusMinutes(1);
        ContentKey contentKey = new ContentKey(tenDaysAgo);
        Webhook webhook = Webhook.builder().ttlMinutes(0).build();
        ChannelConfig channelConfig = ChannelConfig.builder().ttlDays(10).build();
        WebhookLeader.checkExpiration(contentKey, channelConfig, webhook);
    }

}
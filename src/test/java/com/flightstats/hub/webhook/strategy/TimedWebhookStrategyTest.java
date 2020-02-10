package com.flightstats.hub.webhook.strategy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flightstats.hub.cluster.ClusterCacheDao;
import com.flightstats.hub.dao.aws.ContentRetriever;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.ContentPath;
import com.flightstats.hub.model.MinutePath;
import com.flightstats.hub.model.SecondPath;
import com.flightstats.hub.webhook.Webhook;
import org.hamcrest.Matchers;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static com.flightstats.hub.constant.ZookeeperNodes.WEBHOOK_LAST_COMPLETED;
import static com.flightstats.hub.model.WebhookType.MINUTE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TimedWebhookStrategyTest {
//    @BeforeAll
//    static void setupIntegration() {
//        IntegrationTestSetup.run();
//    }

    @Test
    void testRoundingSecondPath() {
        compare(new DateTime(2016, 4, 21, 17, 22, 0, 0, DateTimeZone.UTC), "2016-04-21T17:21:00.000Z");
        compare(new DateTime(2016, 4, 21, 17, 22, 1, 0, DateTimeZone.UTC), "2016-04-21T17:21:00.000Z");
        compare(new DateTime(2016, 4, 21, 17, 22, 58, 0, DateTimeZone.UTC), "2016-04-21T17:21:00.000Z");
        compare(new DateTime(2016, 4, 21, 17, 22, 59, 0, DateTimeZone.UTC), "2016-04-21T17:22:00.000Z");
    }

    private void compare(DateTime start, String expected) {
        SecondPath secondPath = new SecondPath(start);
        DateTime stable = TimedWebhookStrategy.replicatingStable_minute(secondPath);
        assertEquals(expected, stable.toString(), start.toString());
    }

    @Test
    void testRoundingContentKey() {
        compareContentKey(new DateTime(2016, 4, 21, 17, 22, 0, 0, DateTimeZone.UTC), "2016-04-21T17:21:00.000Z");
        compareContentKey(new DateTime(2016, 4, 21, 17, 22, 1, 0, DateTimeZone.UTC), "2016-04-21T17:21:00.000Z");
        compareContentKey(new DateTime(2016, 4, 21, 17, 22, 58, 0, DateTimeZone.UTC), "2016-04-21T17:21:00.000Z");
        compareContentKey(new DateTime(2016, 4, 21, 17, 22, 59, 0, DateTimeZone.UTC), "2016-04-21T17:21:00.000Z");
    }

    private void compareContentKey(DateTime start, String expected) {
        ContentKey secondPath = new ContentKey(start);
        DateTime stable = TimedWebhookStrategy.replicatingStable_minute(secondPath);
        assertEquals(expected, stable.toString(), start.toString());
    }

    @Test
    void testRoundingMinutePath() {
        compareContentPath(new DateTime(2016, 4, 21, 17, 0, 59, 0, DateTimeZone.UTC), "2016-04-21T16:59:00.000Z");
        compareContentPath(new DateTime(2016, 4, 21, 17, 1, 58, 0, DateTimeZone.UTC), "2016-04-21T17:00:00.000Z");
        compareContentPath(new DateTime(2016, 4, 21, 17, 58, 1, 0, DateTimeZone.UTC), "2016-04-21T17:57:00.000Z");
        compareContentPath(new DateTime(2016, 4, 21, 17, 59, 59, 0, DateTimeZone.UTC), "2016-04-21T17:58:00.000Z");
    }

    @Test
    void testStartingPath_minuteBasedWebhookWithAStartingContentKey_returnsChannelLatestOrWhateverZkTellsUs() {
        ContentRetriever contentRetriever = mock(ContentRetriever.class);
        ClusterCacheDao clusterCacheDao = mock(ClusterCacheDao.class);
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        ContentPath startingPath = ContentPath.fromUrl("2019/02/04/13/23/00/930/abcdef").get();
        Webhook webhook = Webhook.builder()
                .name("webhook")
                .channelUrl("http://hi.com")
                .callbackUrl("http://hi-webhook.com")
                .batch(MINUTE.name())
                .parallelCalls(2)
                .startingKey(startingPath)
                .build();

        ContentPath mostRecentKey = ContentKey.fromUrl("2019/02/04/13/25/00/999/adbdsa").get();
        when(clusterCacheDao.get("webhook", startingPath, WEBHOOK_LAST_COMPLETED)).thenReturn(mostRecentKey);

        TimedWebhookStrategy timedWebhookStrategy = new TimedWebhookStrategy(contentRetriever, clusterCacheDao, objectMapper, webhook);

        assertEquals(mostRecentKey, timedWebhookStrategy.getStartingPath());

    }

    @Test
    void testStartingPath_withMinuteBasedWebhookAndNoStartingPath_returnsChannelLatestOrWhateverZkTellsUs() {
        ContentRetriever contentRetriever = mock(ContentRetriever.class);
        ClusterCacheDao clusterCacheDao = mock(ClusterCacheDao.class);
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        Webhook webhook = Webhook.builder()
                .name("webhook")
                .channelUrl("http://hi.com")
                .callbackUrl("http://hi-webhook.com")
                .batch(MINUTE.name())
                .parallelCalls(2)
                .startingKey(null)
                .build();

        ContentPath channelLatest = ContentPath.fromUrl("2019/03/02/21/22/21/000/abcdef").get();
        when(clusterCacheDao.get("webhook", new MinutePath(), WEBHOOK_LAST_COMPLETED)).thenReturn(channelLatest);

        TimedWebhookStrategy timedWebhookStrategy = new TimedWebhookStrategy(contentRetriever, clusterCacheDao, objectMapper, webhook);

        assertEquals(channelLatest, timedWebhookStrategy.getStartingPath());

    }

    private void compareContentPath(DateTime start, String expected) {
        ContentKey secondPath = new ContentKey(start);
        DateTime stable = TimedWebhookStrategy.replicatingStable_minute(secondPath);
        assertEquals(expected, stable.toString(), start.toString());
    }
}
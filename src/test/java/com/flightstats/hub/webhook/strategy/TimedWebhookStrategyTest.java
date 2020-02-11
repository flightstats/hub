package com.flightstats.hub.webhook.strategy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flightstats.hub.cluster.ClusterCacheDao;
import com.flightstats.hub.dao.aws.ContentRetriever;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.ContentPath;
import com.flightstats.hub.model.DurationBasedContentPath;
import com.flightstats.hub.model.Epoch;
import com.flightstats.hub.model.MinutePath;
import com.flightstats.hub.model.SecondPath;
import com.flightstats.hub.model.TimeQuery;
import com.flightstats.hub.util.TimeUtil;
import com.flightstats.hub.webhook.Webhook;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Minutes;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
        SecondPath fourthPath = new SecondPath(start);
        DateTime stable = TimedWebhookStrategy.replicatingStable_minute(fourthPath);
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
        ContentKey fourthPath = new ContentKey(start);
        DateTime stable = TimedWebhookStrategy.replicatingStable_minute(fourthPath);
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

    @Test
    public void testStart_goldenPathForMinuteBasedWebhook_loopsThroughSecondsUntilStableTime() throws Exception {
        ContentRetriever contentRetriever = mock(ContentRetriever.class);
        ClusterCacheDao clusterCacheDao = mock(ClusterCacheDao.class);
        ObjectMapper objectMapper = mock(ObjectMapper.class);


        DateTime defaultStableTime = TimeUtil.stable().minus(Minutes.ONE);

        DateTime startingTime = defaultStableTime.minusMinutes(5);
        ContentPath startingPath = ContentKey.lastKey(startingTime);
        Webhook webhook = Webhook.builder()
                .name("webhook")
                .channelUrl("http://hi.com/channel/channel")
                .callbackUrl("http://hi-webhook.com")
                .batch(MINUTE.name())
                .parallelCalls(3)
                .startingKey(startingPath)
                .build();

        ContentPath lastCompletedPath = ContentPath.fromUrl(String.format("%s/30/234/abcdef", TimeUtil.minutes(startingTime))).get();

        DurationBasedContentPath zerothMinute = buildContentPath(startingTime, Stream.empty());
        DurationBasedContentPath firstMinute = buildContentPath(startingTime.plusMinutes(1), Stream.of("04/000/1234", "06/123/12345"));
        DurationBasedContentPath secondMinute = buildContentPath(startingTime.plusMinutes(2), Stream.empty());
        DurationBasedContentPath thirdMinute = buildContentPath(startingTime.plusMinutes(3), Stream.empty());
        DurationBasedContentPath fourthMinute = buildContentPath(startingTime.plusMinutes(4), Stream.of("06/123/543"));
        DurationBasedContentPath fifthMinute = buildContentPath(startingTime.plusMinutes(5), Stream.empty());

        when(contentRetriever.isLiveChannel("channel")).thenReturn(true);
//        when(contentRetriever.getLastUpdated("channel", MinutePath.NONE)).thenReturn("2019/02/04/12/44")

        when(contentRetriever.queryByTime(buildTimeQuery(firstMinute.getTime())))
                .thenReturn(new TreeSet<>(firstMinute.getKeys()));
        when(contentRetriever.queryByTime(buildTimeQuery(secondMinute.getTime())))
                .thenReturn(new TreeSet<>(secondMinute.getKeys()));
        when(contentRetriever.queryByTime(buildTimeQuery(thirdMinute.getTime())))
                .thenReturn(new TreeSet<>(thirdMinute.getKeys()));
        when(contentRetriever.queryByTime(buildTimeQuery(fourthMinute.getTime())))
                .thenReturn(new TreeSet<>(fourthMinute.getKeys()));
        when(contentRetriever.queryByTime(buildTimeQuery(fifthMinute.getTime())))
                .thenReturn(new TreeSet<>(fifthMinute.getKeys()));

        TimedWebhookStrategy timedWebhookStrategy = new TimedWebhookStrategy(contentRetriever, clusterCacheDao, objectMapper, webhook);
        Runnable contentKeyGenerator = timedWebhookStrategy.getContentKeyGenerator(webhook, lastCompletedPath);
        contentKeyGenerator.run();

        SortedSet<DurationBasedContentPath> queuedPaths = timedWebhookStrategy.drainQueue();
        SortedSet<DurationBasedContentPath> expectedPaths = Stream
                .of(zerothMinute, firstMinute, secondMinute, thirdMinute, fourthMinute, fifthMinute)
                .collect(Collectors.toCollection(TreeSet::new));
        assertEquals(expectedPaths, queuedPaths);
    }

    private TimeQuery buildTimeQuery(DateTime startTime) {
        return TimeQuery.builder()
                .channelName("channel")
                .startTime(startTime)
                .unit(TimeUtil.Unit.MINUTES)
                .stable(true)
                .epoch(Epoch.IMMUTABLE)
                .build();
    }

    private SortedSet<ContentKey> buildKeySet(DateTime time, Stream<String> contentKeys) {
        return contentKeys
                .map(contentKey -> String.format("%s/%s", TimeUtil.minutes(time), contentKey))
                .map(ContentKey::fromUrl)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    private DurationBasedContentPath buildContentPath(DateTime time, Stream<String> contentKeys) {
        return new MinutePath(time, buildKeySet(time, contentKeys));
    }

    private void compareContentPath(DateTime start, String expected) {
        ContentKey fourthPath = new ContentKey(start);
        DateTime stable = TimedWebhookStrategy.replicatingStable_minute(fourthPath);
        assertEquals(expected, stable.toString(), start.toString());
    }
}
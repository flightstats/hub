package com.flightstats.hub.webhook.strategy;

import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.SecondPath;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.Test;

import java.util.function.BiConsumer;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TimedWebhookStrategyTest {
    @Test
    void testRoundingSecondPath() {
        BiConsumer<DateTime, String> compare = (start, expected) -> {
            SecondPath secondPath = new SecondPath(start);
            DateTime stable = TimedWebhookStrategy.replicatingStable_minute(secondPath);
            assertEquals(expected, stable.toString(), start.toString());
        };

        compare.accept(new DateTime(2016, 4, 21, 17, 22, 0, 0, DateTimeZone.UTC), "2016-04-21T17:21:00.000Z");
        compare.accept(new DateTime(2016, 4, 21, 17, 22, 1, 0, DateTimeZone.UTC), "2016-04-21T17:21:00.000Z");
        compare.accept(new DateTime(2016, 4, 21, 17, 22, 58, 0, DateTimeZone.UTC), "2016-04-21T17:21:00.000Z");
        compare.accept(new DateTime(2016, 4, 21, 17, 22, 59, 0, DateTimeZone.UTC), "2016-04-21T17:22:00.000Z");
    }

    @Test
    void testRoundingContentKey() {
        BiConsumer<DateTime, String> compareContentKey = (DateTime start, String expected) -> {
            ContentKey secondPath = new ContentKey(start);
            DateTime stable = TimedWebhookStrategy.replicatingStable_minute(secondPath);
            assertEquals(expected, stable.toString(), start.toString());
        };

        compareContentKey.accept(new DateTime(2016, 4, 21, 17, 22, 0, 0, DateTimeZone.UTC), "2016-04-21T17:21:00.000Z");
        compareContentKey.accept(new DateTime(2016, 4, 21, 17, 22, 1, 0, DateTimeZone.UTC), "2016-04-21T17:21:00.000Z");
        compareContentKey.accept(new DateTime(2016, 4, 21, 17, 22, 58, 0, DateTimeZone.UTC), "2016-04-21T17:21:00.000Z");
        compareContentKey.accept(new DateTime(2016, 4, 21, 17, 22, 59, 0, DateTimeZone.UTC), "2016-04-21T17:21:00.000Z");
    }


    @Test
    void testRoundingMinutePath() {
        BiConsumer<DateTime, String> compareContentPath = (start, expected) -> {
            ContentKey secondPath = new ContentKey(start);
            DateTime stable = TimedWebhookStrategy.replicatingStable_minute(secondPath);
            assertEquals(expected, stable.toString(), start.toString());
        };

        compareContentPath.accept(new DateTime(2016, 4, 21, 17, 0, 59, 0, DateTimeZone.UTC), "2016-04-21T16:59:00.000Z");
        compareContentPath.accept(new DateTime(2016, 4, 21, 17, 1, 58, 0, DateTimeZone.UTC), "2016-04-21T17:00:00.000Z");
        compareContentPath.accept(new DateTime(2016, 4, 21, 17, 58, 1, 0, DateTimeZone.UTC), "2016-04-21T17:57:00.000Z");
        compareContentPath.accept(new DateTime(2016, 4, 21, 17, 59, 59, 0, DateTimeZone.UTC), "2016-04-21T17:58:00.000Z");
    }
}
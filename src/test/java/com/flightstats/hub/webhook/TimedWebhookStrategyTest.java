package com.flightstats.hub.webhook;

import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.SecondPath;
import com.flightstats.hub.test.Integration;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;

public class TimedWebhookStrategyTest {
    @BeforeClass
    public static void setupIntegration() {
        Logger logger = LoggerFactory.getLogger(TimedWebhookStrategyTest.class);
        try {
            Integration.startAwsHub();
        } catch (Exception e) {
            logger.error("failed to start hub for integration test!", e);
        }
    }

    @Test
    public void testRoundingSecondPath() {
        compare(new DateTime(2016, 4, 21, 17, 22, 0, 0, DateTimeZone.UTC), "2016-04-21T17:21:00.000Z");
        compare(new DateTime(2016, 4, 21, 17, 22, 1, 0, DateTimeZone.UTC), "2016-04-21T17:21:00.000Z");
        compare(new DateTime(2016, 4, 21, 17, 22, 58, 0, DateTimeZone.UTC), "2016-04-21T17:21:00.000Z");
        compare(new DateTime(2016, 4, 21, 17, 22, 59, 0, DateTimeZone.UTC), "2016-04-21T17:22:00.000Z");
    }

    private void compare(DateTime start, String expected) {
        SecondPath secondPath = new SecondPath(start);
        DateTime stable = TimedWebhookStrategy.replicatingStable_minute(secondPath);
        assertEquals(start.toString(), expected, stable.toString());
    }

    @Test
    public void testRoundingContentKey() {
        compareContentKey(new DateTime(2016, 4, 21, 17, 22, 0, 0, DateTimeZone.UTC), "2016-04-21T17:21:00.000Z");
        compareContentKey(new DateTime(2016, 4, 21, 17, 22, 1, 0, DateTimeZone.UTC), "2016-04-21T17:21:00.000Z");
        compareContentKey(new DateTime(2016, 4, 21, 17, 22, 58, 0, DateTimeZone.UTC), "2016-04-21T17:21:00.000Z");
        compareContentKey(new DateTime(2016, 4, 21, 17, 22, 59, 0, DateTimeZone.UTC), "2016-04-21T17:21:00.000Z");
    }

    private void compareContentKey(DateTime start, String expected) {
        ContentKey secondPath = new ContentKey(start);
        DateTime stable = TimedWebhookStrategy.replicatingStable_minute(secondPath);
        assertEquals(start.toString(), expected, stable.toString());
    }

}
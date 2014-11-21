package com.flightstats.hub.dao.s3;

import com.flightstats.hub.util.TimeUtil;
import org.joda.time.DateTime;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class S3ChannelWriterTest {

    @Test
    public void testSleepTime() throws Exception {
        DateTime now = TimeUtil.now().withMillis(0).withSecondOfMinute(0);
        assertEquals(0, S3ChannelWriter.getSleep(now.minusHours(1), now));
        assertEquals(0, S3ChannelWriter.getSleep(now.minusMinutes(2), now));
        assertEquals(10 * 1000, S3ChannelWriter.getSleep(now.minusMinutes(1), now));
        assertEquals(70 * 1000, S3ChannelWriter.getSleep(now, now));
    }
}
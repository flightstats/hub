package com.flightstats.hub.time;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TimeAdjusterTest {

    @Test
    public void testSetOffset() {
        long start = System.currentTimeMillis();
        TimeAdjuster timeAdjuster = new TimeAdjuster();
        assertEquals(start, timeAdjuster.getAdjustedNow(start).getMillis());
        timeAdjuster.setOffset(-10, start);
        assertEquals(start + 10, timeAdjuster.getAdjustedNow(start).getMillis());

        timeAdjuster.setOffset(-5, start);
        assertEquals(start + 10, timeAdjuster.getAdjustedNow(start).getMillis());
        assertEquals(start + 10, timeAdjuster.getAdjustedNow(start + 4).getMillis());
        assertEquals(start + 10, timeAdjuster.getAdjustedNow(start + 5).getMillis());
        assertEquals(start + 11, timeAdjuster.getAdjustedNow(start + 6).getMillis());

    }

    @Test
    public void testSetOffsetPositive() {
        long start = System.currentTimeMillis();
        TimeAdjuster timeAdjuster = new TimeAdjuster();
        assertEquals(start, timeAdjuster.getAdjustedNow(start).getMillis());
        timeAdjuster.setOffset(10, start);
        assertEquals(start, timeAdjuster.getAdjustedNow(start).getMillis());
        assertEquals(start, timeAdjuster.getAdjustedNow(start + 10).getMillis());
        assertEquals(start + 1, timeAdjuster.getAdjustedNow(start + 11).getMillis());

        timeAdjuster.setOffset(5, start);
        assertEquals(start, timeAdjuster.getAdjustedNow(start).getMillis());
        assertEquals(start, timeAdjuster.getAdjustedNow(start + 5).getMillis());
        assertEquals(start + 1, timeAdjuster.getAdjustedNow(start + 6).getMillis());

    }
}
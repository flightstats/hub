package com.flightstats.hub.util;

import org.joda.time.DateTime;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TimeIntervalTest {

    @Test
    public void testSingle() {
        DateTime start = new DateTime().minusMinutes(5);
        TimeInterval timeInterval = new TimeInterval(start);
        assertTrue(timeInterval.contains(start));
        assertTrue(timeInterval.contains(start.plusMinutes(1)));
        assertFalse(timeInterval.contains(start.minusMinutes(1)));

        assertTrue(timeInterval.overlaps(start, null));
        assertTrue(timeInterval.overlaps(start, start.plusMinutes(1)));
        assertTrue(timeInterval.overlaps(start.minusMinutes(1), start.plusMinutes(1)));
        assertFalse(timeInterval.overlaps(start.minusMinutes(2), start.minusMinutes(1)));
    }

    @Test
    public void testSingleNow() {
        DateTime start = new DateTime();
        TimeInterval timeInterval = new TimeInterval(start);
        assertTrue(timeInterval.contains(start));
        assertTrue(timeInterval.contains(start.plusMinutes(1)));
        assertFalse(timeInterval.contains(start.minusMinutes(1)));

        assertTrue(timeInterval.overlaps(start, null));
        assertTrue(timeInterval.overlaps(start.plusMillis(1), null));
        assertTrue(timeInterval.overlaps(start.minusMillis(1), null));
        assertTrue(timeInterval.overlaps(start, start.plusMinutes(1)));
        assertTrue(timeInterval.overlaps(start.minusMinutes(1), start.plusMinutes(1)));
        assertFalse(timeInterval.overlaps(start.minusMinutes(2), start.minusMinutes(1)));
    }

    @Test
    public void testRange() {
        DateTime start = new DateTime().minusHours(2);
        DateTime end = start.plusHours(1);
        TimeInterval timeInterval = new TimeInterval(start, end);
        assertTrue(timeInterval.contains(start));
        assertTrue(timeInterval.contains(start.plusMinutes(1)));
        assertFalse(timeInterval.contains(start.minusMinutes(1)));

        assertTrue(timeInterval.contains(end.minusMillis(1)));
        assertFalse(timeInterval.contains(end.plusMinutes(1)));
        assertTrue(timeInterval.contains(start.plusMinutes(1)));

        assertTrue(timeInterval.overlaps(start, end));
        assertTrue(timeInterval.overlaps(start, start.plusMinutes(1)));
        assertTrue(timeInterval.overlaps(start.minusMinutes(1), start.plusMinutes(1)));
        assertFalse(timeInterval.overlaps(start.minusMinutes(2), start.minusMinutes(1)));

        assertTrue(timeInterval.overlaps(end.minusMillis(1), end.plusMinutes(1)));
        assertTrue(timeInterval.overlaps(end.minusMinutes(1), end.plusMinutes(1)));
        assertFalse(timeInterval.overlaps(end.plusMinutes(1), end.plusMinutes(2)));
    }

}
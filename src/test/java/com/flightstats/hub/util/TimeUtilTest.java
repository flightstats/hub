package com.flightstats.hub.util;

import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TimeUtilTest {

    @Test
    void testNow() throws Exception {
        assertEquals((double) System.currentTimeMillis(), (double) TimeUtil.now().getMillis(), (double) 500);
    }

    @Test
    void testStable() throws Exception {
        DateTime now = TimeUtil.now();
        DateTime stableOrdering = TimeUtil.stable();
        assertEquals((double) now.minusMillis(now.getMillisOfSecond()).minusSeconds(5).getMillis(),
                (double) stableOrdering.getMillis(), (double) 500);
    }

    @Test
    void testBirthDay() {
        assertEquals("2015-01-01T00:00:00.000Z", TimeUtil.getBirthDay().toString());
    }

    @Test
    void testEarliestTime1Day() {
        DateTime now = TimeUtil.now();
        DateTime earliestTime = TimeUtil.getEarliestTime(1);
        assertEquals((double) now.minusDays(1).getMillis(),
                (double) earliestTime.getMillis(), (double) 500);
    }

    @Test
    void testEarliestTime100Years() {
        DateTime birthDay = TimeUtil.getBirthDay();
        DateTime earliestTime = TimeUtil.getEarliestTime(100 * 365);
        assertEquals((double) birthDay.getMillis(),
                (double) earliestTime.getMillis(), (double) 500);
    }

}
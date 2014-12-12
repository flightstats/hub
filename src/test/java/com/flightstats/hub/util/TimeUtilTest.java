package com.flightstats.hub.util;

import org.joda.time.DateTime;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TimeUtilTest {

    @Test
    public void testNow() throws Exception {
        assertEquals((double) System.currentTimeMillis(), (double) TimeUtil.now().getMillis(), (double) 100);
    }

    @Test
    public void testStable() throws Exception {
        DateTime now = TimeUtil.now();
        DateTime stableOrdering = TimeUtil.stable();
        assertEquals((double) now.minusMillis(now.getMillisOfSecond()).minusSeconds(5).getMillis(),
                (double) stableOrdering.getMillis(), (double) 100);
    }

}
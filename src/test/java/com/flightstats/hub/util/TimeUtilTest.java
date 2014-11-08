package com.flightstats.hub.util;

import org.joda.time.DateTime;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TimeUtilTest {

    @Test
    public void testNow() throws Exception {
        DateTime now = TimeUtil.now();
        System.out.println(now);
        assertEquals((double) System.currentTimeMillis(), (double) now.getMillis(), (double) 100);
    }

}
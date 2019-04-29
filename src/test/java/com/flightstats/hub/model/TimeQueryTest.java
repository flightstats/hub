package com.flightstats.hub.model;

import com.flightstats.hub.util.TimeUtil;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TimeQueryTest {
    @Test
    void testDefault() throws Exception {
        TimeQuery timeQuery = TimeQuery.builder().channelName("stuff").build();
        assertEquals(Location.ALL, timeQuery.getLocation());
    }

    @Test
    void testOutsideOfCache() {
        DateTime start = TimeUtil.now();
        TimeQuery query = TimeQuery.builder().startTime(start).build();
        assertTrue(query.outsideOfCache(start));
        assertTrue(query.outsideOfCache(start.plusMillis(1)));
        assertFalse(query.outsideOfCache(start.minusMillis(1)));
    }

}
package com.flightstats.hub.model;

import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MinutePathTest {

    @Test
    void testCycleBytes() {
        MinutePath minutePath = new MinutePath();
        ContentPath cycled = minutePath.fromBytes(minutePath.toBytes());
        assertEquals(0, minutePath.compareTo(cycled));
    }

    @Test
    void testCycleZk() {
        MinutePath minutePath = new MinutePath();
        ContentPath cycled = minutePath.fromZk(minutePath.toZk());
        assertEquals(0, minutePath.compareTo(cycled));
    }

    @Test
    void testToUrl() {
        MinutePath minutePath = new MinutePath(new DateTime(123456789));
        assertEquals("1970/01/02/10/17", minutePath.toUrl());
    }

    @Test
    void testCompareContentKey() {
        MinutePath minutePath = new MinutePath();
        ContentKey contentKey = new ContentKey(minutePath.getTime(), "0");
        assertTrue(minutePath.compareTo(contentKey) > 0);

        ContentKey nextSeconds = new ContentKey(minutePath.getTime().plusSeconds(59), "0");
        assertTrue(minutePath.compareTo(nextSeconds) > 0);

        ContentKey nextMinute = new ContentKey(minutePath.getTime().plusMinutes(1), "0");
        assertTrue(minutePath.compareTo(nextMinute) < 0);
    }
}
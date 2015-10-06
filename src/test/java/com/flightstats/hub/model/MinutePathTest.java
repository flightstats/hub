package com.flightstats.hub.model;

import org.joda.time.DateTime;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MinutePathTest {

    @Test
    public void testCycleBytes() {
        MinutePath minutePath = new MinutePath();
        ContentPath cycled = minutePath.fromBytes(minutePath.toBytes());
        assertEquals(0, minutePath.compareTo(cycled));
    }

    @Test
    public void testCycleZk() {
        MinutePath minutePath = new MinutePath();
        ContentPath cycled = minutePath.fromZk(minutePath.toZk());
        assertEquals(0, minutePath.compareTo(cycled));
    }

    @Test
    public void testToUrl() {
        MinutePath minutePath = new MinutePath(new DateTime(123456789));
        assertEquals("1970/01/02/10/17", minutePath.toUrl());
    }

    @Test
    public void testCompareContentKey() {
        MinutePath minutePath = new MinutePath();

        ContentKey before = new ContentKey(minutePath.getTime().minusMinutes(1), "A");
        ContentKey same = new ContentKey(minutePath.getTime(), "A");
        ContentKey after = new ContentKey(minutePath.getTime().plusMinutes(1), "A");

        assertTrue(minutePath.compareTo(before) > 0);
        assertTrue(minutePath.compareTo(after) < 0);
    }
}
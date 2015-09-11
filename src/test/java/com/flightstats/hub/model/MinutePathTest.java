package com.flightstats.hub.model;

import org.joda.time.DateTime;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MinutePathTest {

    @Test
    public void testCycleBytes() {
        MinutePath minutePath = new MinutePath();
        ContentPath cycled = minutePath.toContentPath(minutePath.toBytes());
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
}
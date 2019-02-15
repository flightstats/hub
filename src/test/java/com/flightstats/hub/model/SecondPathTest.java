package com.flightstats.hub.model;

import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SecondPathTest {

    @Test
    public void testCycleBytes() {
        SecondPath path = new SecondPath();
        ContentPath cycled = path.fromBytes(path.toBytes());
        assertEquals(0, path.compareTo(cycled));
    }

    @Test
    public void testCycleZk() {
        SecondPath path = new SecondPath();
        ContentPath cycled = path.fromZk(path.toZk());
        assertEquals(0, path.compareTo(cycled));
    }

    @Test
    public void testToUrl() {
        SecondPath path = new SecondPath(new DateTime(123456789));
        assertEquals("1970/01/02/10/17/36", path.toUrl());
    }

    @Test
    public void testCompareContentKey() {
        SecondPath secondPath = new SecondPath();
        ContentKey contentKey = new ContentKey(secondPath.getTime(), "0");
        assertTrue(secondPath.compareTo(contentKey) > 0);

        ContentKey nextSeconds = new ContentKey(secondPath.getTime().plusMillis(999), "0");
        System.out.println("comparing " + secondPath.toUrl() + " to " + nextSeconds.toUrl());
        assertTrue(secondPath.compareTo(nextSeconds) > 0);

        ContentKey nextMinute = new ContentKey(secondPath.getTime().plusMinutes(1), "0");
        assertTrue(secondPath.compareTo(nextMinute) < 0);
    }

    @Test
    public void testCompareMinutePath() {
        DateTime time = new DateTime().withSecondOfMinute(1);

        SecondPath secondPath = new SecondPath(time);
        MinutePath minutePath = new MinutePath(secondPath.getTime());
        assertTrue(secondPath.compareTo(minutePath) > 0);

       /* MinutePath nextSeconds = new MinutePath(secondPath.getTime().plusSeconds(59));
        assertTrue(secondPath.compareTo(nextSeconds) > 0);

        MinutePath nextMinute = new MinutePath(secondPath.getTime().plusMinutes(1));
        assertTrue(secondPath.compareTo(nextMinute) < 0);*/
    }
}
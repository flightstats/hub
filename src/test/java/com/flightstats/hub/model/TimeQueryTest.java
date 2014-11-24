package com.flightstats.hub.model;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TimeQueryTest {
    @Test
    public void testDefault() throws Exception {
        TimeQuery timeQuery = TimeQuery.builder().channelName("stuff").build();
        assertEquals(TimeQuery.Location.ALL, timeQuery.getLocation());
    }

}
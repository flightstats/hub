package com.flightstats.hub.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ObjectRingTest {
    @Test
    public void testCycle() {
        ObjectRing<String> objectRing = new ObjectRing(5);
        for (int i = 0; i < 20; i++) {
            objectRing.put("i=" + i);
        }
        Object[] items = objectRing.getItems();
        assertEquals("i=15", items[0]);
        assertEquals("i=17", items[2]);
        assertEquals("i=19", items[4]);
    }
}
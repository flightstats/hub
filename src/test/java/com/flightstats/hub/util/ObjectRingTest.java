package com.flightstats.hub.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ObjectRingTest {
    @Test
    public void testCycle() {
        ObjectRing<String> objectRing = new ObjectRing(5);
        for (int i = 0; i < 20; i++) {
            objectRing.put("i=" + i);
        }
        List<String> items = objectRing.getItems();
        assertEquals("i=15", items.get(0));
        assertEquals("i=17", items.get(2));
        assertEquals("i=19", items.get(4));
    }
}
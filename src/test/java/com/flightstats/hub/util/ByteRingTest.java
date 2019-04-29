package com.flightstats.hub.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ByteRingTest {

    @Test
    void testCompare() {
        ByteRing byteRing = new ByteRing(8);
        for (int i = 0; i < 8; i++) {
            byteRing.put((byte) i);
        }
        assertTrue(byteRing.compare(byteRing.getBuffer()));
        assertTrue(byteRing.compare(new byte[]{4, 5, 6, 7}));
    }

    @Test
    void testCompareSmall() {
        ByteRing byteRing = new ByteRing(8);
        for (int i = 0; i < 4; i++) {
            byteRing.put((byte) i);
        }
        assertFalse(byteRing.compare(byteRing.getBuffer()));
        assertTrue(byteRing.compare(new byte[]{-1, 0, 1, 2, 3}));
    }

    @Test
    void testCircleCompare() {
        ByteRing byteRing = new ByteRing(8);
        for (int i = 0; i < 12; i++) {
            byteRing.put((byte) i);
        }
        assertFalse(byteRing.compare(byteRing.getBuffer()));
        assertTrue(byteRing.compare(new byte[]{5, 6, 7, 8, 9, 10, 11}));
    }
}
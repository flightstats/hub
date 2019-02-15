package com.flightstats.hub.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ChunkTest {

    @Test
    public void testPartial() {
        Chunk chunk = new Chunk(1, 4);
        byte[] expected = {1, 2, 3};
        for (byte b : expected) {
            chunk.add(b);
        }
        assertFalse(chunk.isFull());
        assertArrayEquals(expected, chunk.getBytes());
    }

    @Test
    public void testFull() {
        Chunk chunk = new Chunk(1, 4);
        byte[] expected = {1, 2, 3, 4};
        for (byte b : expected) {
            chunk.add(b);
        }
        assertTrue(chunk.isFull());
        assertFalse(chunk.add(5));
        assertArrayEquals(expected, chunk.getBytes());
    }
}
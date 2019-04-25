package com.flightstats.hub.util;

import org.junit.Test;

import java.util.function.Function;

import static org.junit.Assert.assertEquals;

public class ChunkOutputStreamTest {

    private static final int MEGABYTES = 1024 * 1024;
    private ChunkOutputStream chunkOutputStream;

    private Function testFunction = (chunk) -> {
        return "ok";
    };

    @Test
    public void testSize10() {
        chunkOutputStream = new ChunkOutputStream(3, 10, testFunction);
        assertEquals(5 * MEGABYTES, chunkOutputStream.getSize(1));
        assertEquals(10 * MEGABYTES, chunkOutputStream.getSize(4));
        assertEquals(10 * MEGABYTES, chunkOutputStream.getSize(7));
        assertEquals(10 * MEGABYTES, chunkOutputStream.getSize(10));
        assertEquals(10 * MEGABYTES, chunkOutputStream.getSize(13));
        assertEquals(10 * MEGABYTES, chunkOutputStream.getSize(16));
    }

    @Test
    public void testSize20() {
        chunkOutputStream = new ChunkOutputStream(3, 20, testFunction);
        assertEquals(5 * MEGABYTES, chunkOutputStream.getSize(1));
        assertEquals(10 * MEGABYTES, chunkOutputStream.getSize(4));
        assertEquals(15 * MEGABYTES, chunkOutputStream.getSize(7));
        assertEquals(20 * MEGABYTES, chunkOutputStream.getSize(10));
        assertEquals(20 * MEGABYTES, chunkOutputStream.getSize(13));
        assertEquals(20 * MEGABYTES, chunkOutputStream.getSize(16));
    }

    @Test
    public void testSize50() {
        chunkOutputStream = new ChunkOutputStream(3, 50, testFunction);
        assertEquals(5 * MEGABYTES, chunkOutputStream.getSize(1));
        assertEquals(10 * MEGABYTES, chunkOutputStream.getSize(4));
        assertEquals(15 * MEGABYTES, chunkOutputStream.getSize(7));
        assertEquals(20 * MEGABYTES, chunkOutputStream.getSize(10));
        assertEquals(25 * MEGABYTES, chunkOutputStream.getSize(13));
        assertEquals(30 * MEGABYTES, chunkOutputStream.getSize(16));
        assertEquals(50 * MEGABYTES, chunkOutputStream.getSize(30));
    }

}
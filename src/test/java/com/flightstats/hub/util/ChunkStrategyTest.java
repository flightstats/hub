package com.flightstats.hub.util;

import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.test.TestMain;
import com.google.inject.Injector;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ChunkStrategyTest {

    private static final int MEGABYTES = 1024 * 1024;
    private HubProperties hubProperties;

    @BeforeClass
    public void setUpClass() throws Exception {
        Injector injector = TestMain.start();
        hubProperties = injector.getInstance(HubProperties.class);
    }

    @Test
    public void testSize10() {

        hubProperties.setProperty("s3.maxChunkMB", "10");
        assertEquals(5 * MEGABYTES, ChunkStrategy.getSize(1));
        assertEquals(10 * MEGABYTES, ChunkStrategy.getSize(4));
        assertEquals(10 * MEGABYTES, ChunkStrategy.getSize(7));
        assertEquals(10 * MEGABYTES, ChunkStrategy.getSize(10));
        assertEquals(10 * MEGABYTES, ChunkStrategy.getSize(13));
        assertEquals(10 * MEGABYTES, ChunkStrategy.getSize(16));
    }

    @Test
    public void testSize20() {
        hubProperties.setProperty("s3.maxChunkMB", "20");
        assertEquals(5 * MEGABYTES, ChunkStrategy.getSize(1));
        assertEquals(10 * MEGABYTES, ChunkStrategy.getSize(4));
        assertEquals(15 * MEGABYTES, ChunkStrategy.getSize(7));
        assertEquals(20 * MEGABYTES, ChunkStrategy.getSize(10));
        assertEquals(20 * MEGABYTES, ChunkStrategy.getSize(13));
        assertEquals(20 * MEGABYTES, ChunkStrategy.getSize(16));
    }

    @Test
    public void testSize50() {
        hubProperties.setProperty("s3.maxChunkMB", "50");
        assertEquals(5 * MEGABYTES, ChunkStrategy.getSize(1));
        assertEquals(10 * MEGABYTES, ChunkStrategy.getSize(4));
        assertEquals(15 * MEGABYTES, ChunkStrategy.getSize(7));
        assertEquals(20 * MEGABYTES, ChunkStrategy.getSize(10));
        assertEquals(25 * MEGABYTES, ChunkStrategy.getSize(13));
        assertEquals(30 * MEGABYTES, ChunkStrategy.getSize(16));
        assertEquals(50 * MEGABYTES, ChunkStrategy.getSize(30));
    }

}

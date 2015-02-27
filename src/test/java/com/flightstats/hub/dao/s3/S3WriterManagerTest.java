package com.flightstats.hub.dao.s3;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class S3WriterManagerTest {
    @Test
    public void testServerOffset() throws Exception {
        assertEquals(45, S3WriterManager.serverOffset("hub-v2-01.hub"));
        assertEquals(30, S3WriterManager.serverOffset("hub-v2-02.hub"));
        assertEquals(15, S3WriterManager.serverOffset("hub-v2-03.hub"));
    }
}
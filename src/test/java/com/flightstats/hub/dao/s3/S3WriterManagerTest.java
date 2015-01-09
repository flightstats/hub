package com.flightstats.hub.dao.s3;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class S3WriterManagerTest {
    @Test
    public void testServerOffset() throws Exception {
        assertEquals(15, S3WriterManager.serverOffset("hub-v2-01.cloud-east.dev"));
        assertEquals( 30, S3WriterManager.serverOffset("hub-v2-02.cloud-east.dev"));
        assertEquals( 45, S3WriterManager.serverOffset("hub-v2-03.cloud-east.dev"));
    }
}
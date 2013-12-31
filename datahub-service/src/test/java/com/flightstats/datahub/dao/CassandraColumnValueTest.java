package com.flightstats.datahub.dao;

import com.flightstats.datahub.model.Content;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CassandraColumnValueTest {

    @Test
    public void testDataLength() throws Exception {
        Content testClass = new Content(null, null, new byte[8], 0L);
        assertEquals(8, testClass.getDataLength());
    }

    @Test
    public void testDataLength_null() throws Exception {
        Content testClass = new Content(null, null, null, 0L);
        assertEquals(0, testClass.getDataLength());
    }
}

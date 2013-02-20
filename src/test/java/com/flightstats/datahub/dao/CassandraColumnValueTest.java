package com.flightstats.datahub.dao;

import com.flightstats.datahub.model.DataHubCompositeValue;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;

public class CassandraColumnValueTest {

    @Test
    public void testContentLength() throws Exception {
        DataHubCompositeValue testClass = new DataHubCompositeValue("text/plain", null);
        assertEquals(10, testClass.getContentTypeLength());
    }

    @Test
    public void testContentLength_null() throws Exception {
        DataHubCompositeValue testClass = new DataHubCompositeValue(null, null);
        assertEquals(0, testClass.getContentTypeLength());
    }

    @Test
    public void testDataLength() throws Exception {
        DataHubCompositeValue testClass = new DataHubCompositeValue(null, new byte[8]);
        assertEquals(8, testClass.getDataLength());
    }

    @Test
    public void testDataLength_null() throws Exception {
        DataHubCompositeValue testClass = new DataHubCompositeValue(null, null);
        assertEquals(0, testClass.getDataLength());
    }
}

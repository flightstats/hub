package com.flightstats.datahub.dao;

import com.flightstats.datahub.model.DataHubCompositeValue;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CassandraColumnValueTest {

    @Test
    public void testDataLength() throws Exception {
        DataHubCompositeValue testClass = new DataHubCompositeValue(null, null, new byte[8]);
        assertEquals(8, testClass.getDataLength());
    }

    @Test
    public void testDataLength_null() throws Exception {
        DataHubCompositeValue testClass = new DataHubCompositeValue(null, null, null);
        assertEquals(0, testClass.getDataLength());
    }
}

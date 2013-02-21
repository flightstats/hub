package com.flightstats.datahub.util;

import com.flightstats.datahub.model.DataHubKey;
import org.junit.Test;

import java.util.Date;

import static junit.framework.TestCase.assertEquals;

public class DataHubKeyRendererTest {

    @Test
    public void testKeyToString() throws Exception {
        DataHubKey key = new DataHubKey(new Date(1361404433284L), (short) 129);
        DataHubKeyRenderer testClass = new DataHubKeyRenderer();
        String result = testClass.keyToString(key);
        assertEquals("00002F7Q0S9O8041", result);
    }

    @Test
    public void testKeyFromString() throws Exception {
        DataHubKey expected = new DataHubKey(new Date(1361404433284L), (short) 129);
        DataHubKeyRenderer testClass = new DataHubKeyRenderer();
        DataHubKey result = testClass.fromString("00002F7Q0S9O8041");
        assertEquals(expected, result);
    }


}

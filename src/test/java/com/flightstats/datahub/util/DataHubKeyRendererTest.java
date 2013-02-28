package com.flightstats.datahub.util;

import com.flightstats.datahub.model.DataHubKey;
import org.junit.Test;

import java.util.Date;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertTrue;

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

    @Test
    public void testSequencesAreOrderedWithinSameMillis() throws Exception {
        Date date = new Date(55555L);
        DataHubKey key1 = new DataHubKey(date, (short) 1);
        DataHubKey key2 = new DataHubKey(date, (short) 2);
        DataHubKey key3 = new DataHubKey(date, (short) 3);

        DataHubKeyRenderer testClass = new DataHubKeyRenderer();

        String string1 = testClass.keyToString(key1);
        String string2 = testClass.keyToString(key2);
        String string3 = testClass.keyToString(key3);

        assertTrue(string1.compareTo(string2) < 0);
        assertTrue(string2.compareTo(string3) < 0);
    }

}

package com.flightstats.datahub.util;

import com.flightstats.datahub.model.DataHubKey;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DataHubKeyRendererTest {

    @Test
    public void testKeyToString() throws Exception {
        DataHubKey key = new DataHubKey((short) 129);
        DataHubKeyRenderer testClass = new DataHubKeyRenderer();
        String result = testClass.keyToString(key);
        assertEquals("0000000000082===", result);
    }

    @Test
    public void testKeyFromString() throws Exception {
        DataHubKey expected = new DataHubKey((short) 129);
        DataHubKeyRenderer testClass = new DataHubKeyRenderer();
        DataHubKey result = testClass.fromString(testClass.keyToString(expected));
        assertEquals(expected, result);
    }

    @Test
    public void testSequencesAreOrderedWithinSameMillis() throws Exception {
        DataHubKey key1 = new DataHubKey((short) 1);
        DataHubKey key2 = new DataHubKey((short) 2);
        DataHubKey key3 = new DataHubKey((short) 3);

        DataHubKeyRenderer testClass = new DataHubKeyRenderer();

        String string1 = testClass.keyToString(key1);
        String string2 = testClass.keyToString(key2);
        String string3 = testClass.keyToString(key3);

        assertTrue(string1.compareTo(string2) < 0);
        assertTrue(string2.compareTo(string3) < 0);
    }

}

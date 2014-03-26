package com.flightstats.hub.model;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 *
 */
public class SequenceContentKeyTest {

    @Test
    public void testFromStringAbsent() throws Exception {
        assertFalse(SequenceContentKey.fromString("1").isPresent());
        assertFalse(SequenceContentKey.fromString("999").isPresent());
    }

    @Test
    public void testFromStringExists() throws Exception {
        assertTrue(SequenceContentKey.fromString("1000").isPresent());
        assertTrue(SequenceContentKey.fromString("99999999").isPresent());
    }
}

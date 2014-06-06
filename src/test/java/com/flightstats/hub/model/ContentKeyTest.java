package com.flightstats.hub.model;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 *
 */
public class ContentKeyTest {

    @Test
    public void testFromStringAbsent() throws Exception {
        assertFalse(ContentKey.fromString("1").isPresent());
        assertFalse(ContentKey.fromString("999").isPresent());
    }

    @Test
    public void testFromStringExists() throws Exception {
        assertTrue(ContentKey.fromString("1000").isPresent());
        assertTrue(ContentKey.fromString("99999999").isPresent());
    }
}

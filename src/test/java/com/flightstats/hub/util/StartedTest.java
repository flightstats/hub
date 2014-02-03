package com.flightstats.hub.util;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 *
 */
public class StartedTest {

    @Test
    public void test() throws Exception {
        Started started = new Started();
        assertFalse(started.isStarted());
        assertTrue(started.isStarted());
        assertTrue(started.isStarted());
    }
}

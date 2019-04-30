package com.flightstats.hub.app;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class HubPropertiesTest {

    @Test
    public void testIsReadOnly() {
        assertFalse(HubProperties.isReadOnly());
        HubProperties.setProperty("hub.read.only", "true");
        assertTrue(HubProperties.isReadOnly());
        HubProperties.setProperty("hub.read.only", "");
        assertFalse(HubProperties.isReadOnly());
    }

}
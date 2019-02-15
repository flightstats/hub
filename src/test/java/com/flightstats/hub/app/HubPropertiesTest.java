package com.flightstats.hub.app;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class HubPropertiesTest {

    @Test
    public void testIsReadOnly() {
        assertFalse(HubProperties.isReadOnly());
        HubProperties.setProperty("hub.read.only", "somewhere," + HubHost.getLocalName());
        assertTrue(HubProperties.isReadOnly());
        HubProperties.setProperty("hub.read.only", "");
    }

}
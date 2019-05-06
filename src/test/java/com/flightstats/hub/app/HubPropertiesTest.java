package com.flightstats.hub.app;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class HubPropertiesTest {

    @Test
    public void testIsReadOnlyIsFalseByDefault() {
        assertFalse(HubProperties.isReadOnly());
    }

    @Test
    public void testIsReadOnlyIsFalseIfNotSpecified() {
        HubProperties.setProperty("hub.read.only", "");
        assertFalse(HubProperties.isReadOnly());
    }

    @Test
    public void testIsReadOnlyIsTrueWhenSetInProps() {
        HubProperties.setProperty("hub.read.only", "true");
        assertTrue(HubProperties.isReadOnly());
    }

}
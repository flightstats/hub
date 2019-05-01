package com.flightstats.hub.app;

import com.flightstats.hub.config.AppProperty;
import com.flightstats.hub.config.PropertyLoader;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PropertyLoaderTest {

    @Test
    void testIsReadOnly() {
        AppProperty appProperty = new AppProperty(PropertyLoader.getInstance());
        assertFalse(appProperty.isReadOnly());
        PropertyLoader.getInstance().setProperty("hub.read.only", "somewhere," + HubHost.getLocalName());
        assertTrue(appProperty.isReadOnly());
        PropertyLoader.getInstance().setProperty("hub.read.only", "");
    }

}
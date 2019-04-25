package com.flightstats.hub.app;

import com.flightstats.hub.config.AppProperty;
import com.flightstats.hub.config.PropertyLoader;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PropertyLoaderTest {

    @Test
    public void testIsReadOnly() {
        AppProperty appProperty = new AppProperty(PropertyLoader.getInstance());
        assertFalse(appProperty.isReadOnly());
        PropertyLoader.getInstance().setProperty("hub.read.only", "somewhere," + HubHost.getLocalName());
        assertTrue(appProperty.isReadOnly());
        PropertyLoader.getInstance().setProperty("hub.read.only", "");
    }

}
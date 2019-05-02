package com.flightstats.hub.app;

import com.flightstats.hub.config.AppProperties;
import com.flightstats.hub.config.PropertiesLoader;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PropertyLoaderTest {

    @Test
    public void testIsReadOnly() {
        AppProperties appProperties = new AppProperties(PropertiesLoader.getInstance());
        assertFalse(appProperties.isReadOnly());
        PropertiesLoader.getInstance().setProperty("hub.read.only", "somewhere," + HubHost.getLocalName());
        assertTrue(appProperties.isReadOnly());
        PropertiesLoader.getInstance().setProperty("hub.read.only", "");
    }

}
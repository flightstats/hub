package com.flightstats.hub.app;

import com.flightstats.hub.config.AppProperties;
import com.flightstats.hub.config.PropertiesLoader;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PropertyLoaderTest {

    @Test
    void testIsReadOnly() {
        AppProperties appProperties = new AppProperties(PropertiesLoader.getInstance());
        assertFalse(appProperties.isReadOnly());
        PropertiesLoader.getInstance().setProperty("hub.read.only", "true");
        assertTrue(appProperties.isReadOnly());
        PropertiesLoader.getInstance().setProperty("hub.read.only", "");
        assertFalse(appProperties.isReadOnly());
    }

}
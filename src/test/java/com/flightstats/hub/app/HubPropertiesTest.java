package com.flightstats.hub.app;

import org.junit.Test;

import java.util.Properties;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class HubPropertiesTest {

    @Test
    public void testReadOnlyIsSet() {
        Properties properties = new Properties();
        properties.setProperty("hub.read.only", "somewhere," + HubHost.getLocalName());
        HubProperties hubProperties = new HubProperties(properties);
        assertTrue(hubProperties.isReadOnly());
    }

    @Test
    public void testReadOnlyIsEmpty() {
        Properties properties = new Properties();
        properties.setProperty("hub.read.only", "");
        HubProperties hubProperties = new HubProperties(properties);
        assertFalse(hubProperties.isReadOnly());
    }

    @Test
    public void testReadOnlyIsNotSet() {
        Properties properties = new Properties();
        HubProperties hubProperties = new HubProperties(properties);
        assertFalse(hubProperties.isReadOnly());
    }

}

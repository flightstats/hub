package com.flightstats.hub.model;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 *
 */
public class ChannelConfigurationTest {

    @Test
    public void testDefaults() throws Exception {
        ChannelConfiguration config = ChannelConfiguration.builder().withName("defaults").build();
        assertDefaults(config);
        ChannelConfiguration copy = ChannelConfiguration.builder().withChannelConfiguration(config).build();
        assertDefaults(copy);
    }

    @Test
    public void testJsonDefaults() throws Exception {
        assertDefaults(ChannelConfiguration.fromJson("{\"name\": \"defaults\"}"));
    }

    private void assertDefaults(ChannelConfiguration config) {
        assertEquals("defaults", config.getName());
        assertEquals(120, config.getTtlDays());
        assertEquals("", config.getDescription());
        assertTrue(config.getTags().isEmpty());
    }

    @Test
    public void testDescription() throws Exception {
        ChannelConfiguration config = ChannelConfiguration.builder().withDescription("some thing").build();
        assertEquals("some thing", config.getDescription());
    }

    @Test
    public void testDescriptionCopy() throws Exception {
        ChannelConfiguration config = ChannelConfiguration.builder().withDescription("some copy").build();
        ChannelConfiguration copy = ChannelConfiguration.builder().withChannelConfiguration(config).build();
        assertEquals("some copy", copy.getDescription());
    }

    @Test
    public void testTags() throws Exception {
        List<String> tags = Arrays.asList("one", "two", "three", "4 four");
        ChannelConfiguration config = ChannelConfiguration.builder().withTags(tags).build();
        assertEquals(4, config.getTags().size());
        assertTrue(config.getTags().containsAll(tags));
    }

    @Test
    public void testTagsCopy() throws Exception {
        List<String> tags = Arrays.asList("one", "two", "three", "4 four");
        ChannelConfiguration config = ChannelConfiguration.builder().withTags(tags).build();
        ChannelConfiguration copy = ChannelConfiguration.builder().withChannelConfiguration(config).build();
        assertTrue(copy.getTags().containsAll(config.getTags()));
    }
}

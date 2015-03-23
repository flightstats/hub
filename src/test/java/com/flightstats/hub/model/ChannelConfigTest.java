package com.flightstats.hub.model;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ChannelConfigTest {

    @Test
    public void testDefaults() throws Exception {
        ChannelConfig config = ChannelConfig.builder().withName("defaults").build();
        assertDefaults(config);
        ChannelConfig copy = ChannelConfig.builder().withChannelConfiguration(config).build();
        assertDefaults(copy);
    }

    @Test
    public void testJsonDefaults() throws Exception {
        assertDefaults(ChannelConfig.fromJson("{\"name\": \"defaults\"}"));
    }

    private void assertDefaults(ChannelConfig config) {
        assertEquals("defaults", config.getName());
        assertEquals(120, config.getTtlDays());
        assertEquals("", config.getDescription());
        assertTrue(config.getTags().isEmpty());
        assertEquals("", config.getReplicationSource());
    }

    @Test
    public void testDescription() throws Exception {
        ChannelConfig config = ChannelConfig.builder().withDescription("some thing").build();
        assertEquals("some thing", config.getDescription());
    }

    @Test
    public void testDescriptionCopy() throws Exception {
        ChannelConfig config = ChannelConfig.builder().withDescription("some copy").build();
        ChannelConfig copy = ChannelConfig.builder().withChannelConfiguration(config).build();
        assertEquals("some copy", copy.getDescription());
    }

    @Test
    public void testTags() throws Exception {
        List<String> tags = Arrays.asList("one", "two", "three", "4 four");
        ChannelConfig config = ChannelConfig.builder().withTags(tags).build();
        assertEquals(4, config.getTags().size());
        assertTrue(config.getTags().containsAll(tags));
    }

    @Test
    public void testTagsCopy() throws Exception {
        List<String> tags = Arrays.asList("one", "two", "three", "4 four");
        ChannelConfig config = ChannelConfig.builder().withTags(tags).build();
        ChannelConfig copy = ChannelConfig.builder().withChannelConfiguration(config).build();
        assertTrue(copy.getTags().containsAll(config.getTags()));
    }

    @Test
    public void testReplicationSource() throws Exception {
        String replicationSource = "http://hub/channel/blah";
        ChannelConfig config = ChannelConfig.builder().withReplicationSource(replicationSource).build();
        assertEquals(replicationSource, config.getReplicationSource());
    }

    @Test
    public void testReplicationSourceCopy() throws Exception {
        String replicationSource = "http://hub/channel/blah";
        ChannelConfig config = ChannelConfig.builder().withReplicationSource(replicationSource).build();
        ChannelConfig copy = ChannelConfig.builder().withChannelConfiguration(config).build();
        assertEquals(replicationSource, copy.getReplicationSource());
    }
}

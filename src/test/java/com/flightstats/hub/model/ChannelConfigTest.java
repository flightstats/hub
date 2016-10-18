package com.flightstats.hub.model;

import com.google.common.collect.Sets;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class ChannelConfigTest {

    @Test
    public void testDefaults() throws Exception {
        ChannelConfig config = ChannelConfig.builder().name("defaults").build();
        assertDefaults(config);
        // TODO: what is this copy attempting to test?
        ChannelConfig copy = config.toBuilder().build();
        assertDefaults(copy);
        assertFalse(config.hasChanged(copy));
    }

    @Test
    public void testJsonDefaults() throws Exception {
        assertDefaults(ChannelConfig.createFromJson("{\"name\": \"defaults\"}"));
    }

    private void assertDefaults(ChannelConfig config) {
        assertEquals("defaults", config.getName());
        assertEquals(120, config.getTtlDays());
        assertEquals("", config.getDescription());
        assertTrue(config.getTags().isEmpty());
        assertEquals("", config.getReplicationSource());
        assertEquals("SINGLE", config.getStorage());
        assertEquals(null, config.getGlobal());
    }

    @Test
    public void testDescription() throws Exception {
        ChannelConfig config = ChannelConfig.builder().description("some thing").build();
        assertEquals("some thing", config.getDescription());
    }

    @Test
    public void testDescriptionCopy() throws Exception {
        ChannelConfig config = ChannelConfig.builder().description("some copy").build();
        // TODO: what is this copy attempting to test?
        ChannelConfig copy = config.toBuilder().build();
        assertEquals("some copy", copy.getDescription());
        assertFalse(config.hasChanged(copy));
    }

    @Test
    public void testTags() throws Exception {
        List<String> tags = Arrays.asList("one", "two", "three", "4 four");
        ChannelConfig config = ChannelConfig.builder().tags(tags).build();
        assertEquals(4, config.getTags().size());
        assertTrue(config.getTags().containsAll(tags));
    }

    @Test
    public void testTagsCopy() throws Exception {
        List<String> tags = Arrays.asList("one", "two", "three", "4 four");
        ChannelConfig config = ChannelConfig.builder().tags(tags).build();
        // TODO: what is this copy attempting to test?
        ChannelConfig copy = config.toBuilder().build();
        assertTrue(copy.getTags().containsAll(config.getTags()));
        assertFalse(config.hasChanged(copy));
    }

    @Test
    public void testReplicationSource() throws Exception {
        String replicationSource = "http://hub/channel/blah";
        ChannelConfig config = ChannelConfig.builder().replicationSource(replicationSource).build();
        assertEquals(replicationSource, config.getReplicationSource());
    }

    @Test
    public void testReplicationSourceCopy() throws Exception {
        String replicationSource = "http://hub/channel/blah";
        ChannelConfig config = ChannelConfig.builder().replicationSource(replicationSource).build();
        // TODO: what is this copy attempting to test?
        ChannelConfig copy = config.toBuilder().build();
        assertEquals(replicationSource, copy.getReplicationSource());
        assertFalse(config.hasChanged(copy));
    }

    @Test
    public void testTypeCopy() {
        ChannelConfig config = ChannelConfig.builder().storage("BOTH").build();
        // TODO: what is this copy attempting to test?
        ChannelConfig copy = config.toBuilder().build();
        assertEquals("BOTH", copy.getStorage());
        assertFalse(config.hasChanged(copy));
    }

    @Test
    public void testHasChanged() {
        ChannelConfig defaults = ChannelConfig.builder().name("defaults").build();
        assertFalse(defaults.hasChanged(defaults));
        ChannelConfig hasCheez = ChannelConfig.builder().name("hasCheez").build();
        assertFalse(defaults.hasChanged(hasCheez));
        ChannelConfig repl = hasCheez.toBuilder().replicationSource("R").build();
        assertTrue(repl.hasChanged(defaults));

        ChannelConfig desc = hasCheez.toBuilder().description("D").build();
        assertTrue(desc.hasChanged(defaults));

        ChannelConfig max = hasCheez.toBuilder().maxItems(1).build();
        assertTrue(max.hasChanged(defaults));

        ChannelConfig owner = hasCheez.toBuilder().owner("O").build();
        assertTrue(owner.hasChanged(defaults));

        ChannelConfig storage = hasCheez.toBuilder().storage("S").build();
        assertTrue(storage.hasChanged(defaults));

        ChannelConfig ttl = hasCheez.toBuilder().ttlDays(5).build();
        assertTrue(ttl.hasChanged(defaults));

        ChannelConfig tags = hasCheez.toBuilder().tags(Sets.newHashSet("one", "two")).build();
        assertTrue(tags.hasChanged(defaults));
    }

    @Test
    public void testGlobalCopy() throws IOException {
        GlobalConfig globalConfig = new GlobalConfig();
        globalConfig.setMaster("master");
        globalConfig.addSatellites(Arrays.asList("sat1", "sat2"));
        ChannelConfig config = ChannelConfig.builder().global(globalConfig).build();
        ChannelConfig copy = config.toBuilder().build();
        assertEquals(globalConfig, copy.getGlobal());
        assertFalse(config.hasChanged(copy));

        ChannelConfig fromJson = ChannelConfig.createFromJson(config.toJson());
        assertEquals(globalConfig, fromJson.getGlobal());

        GlobalConfig changedGlobal = new GlobalConfig();
        changedGlobal.setMaster("sat1");
        changedGlobal.addSatellites(Arrays.asList("master", "sat2", "sat3"));
        ChannelConfig changedChannel = ChannelConfig.builder().global(changedGlobal).build();

        ChannelConfig updated = ChannelConfig.updateFromJson(config, changedChannel.toJson());
        assertEquals(changedGlobal, updated.getGlobal());
    }
}

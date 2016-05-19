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
        ChannelConfig config = ChannelConfig.builder().withName("defaults").build();
        assertDefaults(config);
        ChannelConfig copy = getBuilder(config).build();
        assertDefaults(copy);
        assertFalse(config.hasChanged(copy));
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
        assertEquals("SINGLE", config.getStorage());
        assertEquals(null, config.getGlobal());
    }

    @Test
    public void testDescription() throws Exception {
        ChannelConfig config = ChannelConfig.builder().withDescription("some thing").build();
        assertEquals("some thing", config.getDescription());
    }

    @Test
    public void testDescriptionCopy() throws Exception {
        ChannelConfig config = ChannelConfig.builder().withDescription("some copy").build();
        ChannelConfig copy = getBuilder(config).build();
        assertEquals("some copy", copy.getDescription());
        assertFalse(config.hasChanged(copy));
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
        ChannelConfig copy = getBuilder(config).build();
        assertTrue(copy.getTags().containsAll(config.getTags()));
        assertFalse(config.hasChanged(copy));
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
        ChannelConfig copy = getBuilder(config).build();
        assertEquals(replicationSource, copy.getReplicationSource());
        assertFalse(config.hasChanged(copy));
    }

    @Test
    public void testTypeCopy() {
        ChannelConfig config = ChannelConfig.builder().withStorage("BOTH").build();
        ChannelConfig copy = getBuilder(config).build();
        assertEquals("BOTH", copy.getStorage());
        assertFalse(config.hasChanged(copy));
    }

    @Test
    public void testHasChanged() {
        ChannelConfig defaults = ChannelConfig.builder().withName("defaults").build();
        assertFalse(defaults.hasChanged(defaults));
        ChannelConfig hasCheez = ChannelConfig.builder().withName("hasCheez").build();
        assertFalse(defaults.hasChanged(hasCheez));
        ChannelConfig repl = getBuilder(hasCheez).withReplicationSource("R").build();
        assertTrue(repl.hasChanged(defaults));

        ChannelConfig desc = getBuilder(hasCheez).withDescription("D").build();
        assertTrue(desc.hasChanged(defaults));

        ChannelConfig max = getBuilder(hasCheez).withMaxItems(1).build();
        assertTrue(max.hasChanged(defaults));

        ChannelConfig owner = getBuilder(hasCheez).withOwner("O").build();
        assertTrue(owner.hasChanged(defaults));

        ChannelConfig storage = getBuilder(hasCheez).withStorage("S").build();
        assertTrue(storage.hasChanged(defaults));

        ChannelConfig ttl = getBuilder(hasCheez).withTtlDays(5).build();
        assertTrue(ttl.hasChanged(defaults));

        ChannelConfig tags = getBuilder(hasCheez).withTags(Sets.newHashSet("one", "two")).build();
        assertTrue(tags.hasChanged(defaults));
    }

    @Test
    public void testGlobalCopy() throws IOException {
        GlobalConfig globalConfig = new GlobalConfig();
        globalConfig.setMaster("master");
        globalConfig.getSatellites().addAll(Arrays.asList("sat1", "sat2"));
        ChannelConfig config = ChannelConfig.builder().withGlobal(globalConfig).build();
        ChannelConfig copy = getBuilder(config).build();
        assertEquals(globalConfig, copy.getGlobal());
        assertFalse(config.hasChanged(copy));

        ChannelConfig fromJson = ChannelConfig.fromJson(config.toJson());
        assertEquals(globalConfig, fromJson.getGlobal());

        GlobalConfig changedGlobal = new GlobalConfig();
        changedGlobal.setMaster("sat1");
        changedGlobal.getSatellites().addAll(Arrays.asList("master", "sat2", "sat3"));
        ChannelConfig changedChannel = ChannelConfig.builder().withGlobal(changedGlobal).build();

        ChannelConfig updated = ChannelConfig.builder()
                .withChannelConfiguration(config)
                .withUpdateJson(changedChannel.toJson()).build();
        assertEquals(changedGlobal, updated.getGlobal());
    }

    private ChannelConfig.Builder getBuilder(ChannelConfig config) {
        return ChannelConfig.builder().withChannelConfiguration(config);
    }
}

package com.flightstats.hub.model;

import com.flightstats.hub.util.TimeUtil;
import com.google.common.collect.Sets;
import org.joda.time.DateTime;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class ChannelConfigTest {
    private static final Logger logger = LoggerFactory.getLogger(ChannelConfigTest.class);

    @Test
    public void testDefaults() throws Exception {
        ChannelConfig config = ChannelConfig.builder().name("defaults").build();
        assertDefaults(config);
        ChannelConfig copy = config.toBuilder().build();
        assertDefaults(copy);
        assertTrue(config.equals(copy));
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
        assertEquals(null, config.getMutableTime());
        assertTrue(config.isAllowZeroBytes());
    }

    @Test
    public void testDescription() throws Exception {
        ChannelConfig config = ChannelConfig.builder().description("some thing").build();
        assertEquals("some thing", config.getDescription());
    }

    @Test
    public void testDescriptionCopy() throws Exception {
        ChannelConfig config = ChannelConfig.builder().description("some copy").build();
        ChannelConfig copy = config.toBuilder().build();
        assertEquals("some copy", copy.getDescription());
        assertTrue(config.equals(copy));
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
        ChannelConfig copy = config.toBuilder().build();
        assertTrue(copy.getTags().containsAll(config.getTags()));
        assertTrue(config.equals(copy));
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
        ChannelConfig copy = config.toBuilder().build();
        assertEquals(replicationSource, copy.getReplicationSource());
        assertTrue(config.equals(copy));
    }

    @Test
    public void testTypeCopy() {
        ChannelConfig config = ChannelConfig.builder().storage("BOTH").build();
        ChannelConfig copy = config.toBuilder().build();
        assertEquals("BOTH", copy.getStorage());
        assertTrue(config.equals(copy));
    }

    @Test
    public void testHasChanged() {
        ChannelConfig defaults = ChannelConfig.builder().name("defaults").build();
        ChannelConfig hasCheez = ChannelConfig.builder().name("hasCheez").build();

        ChannelConfig repl = hasCheez.toBuilder().replicationSource("R").build();
        assertFalse(repl.equals(defaults));

        ChannelConfig desc = hasCheez.toBuilder().description("D").build();
        assertFalse(desc.equals(defaults));

        ChannelConfig max = hasCheez.toBuilder().maxItems(1).build();
        assertFalse(max.equals(defaults));

        ChannelConfig owner = hasCheez.toBuilder().owner("O").build();
        assertFalse(owner.equals(defaults));

        ChannelConfig storage = hasCheez.toBuilder().storage("S").build();
        assertFalse(storage.equals(defaults));

        ChannelConfig ttl = hasCheez.toBuilder().ttlDays(5).build();
        assertFalse(ttl.equals(defaults));

        ChannelConfig tags = hasCheez.toBuilder().tags(Sets.newHashSet("one", "two")).build();
        assertFalse(tags.equals(defaults));
    }

    @Test
    public void testCopy() throws IOException {
        ChannelConfig config = ChannelConfig.builder()
                .owner("ABC")
                .description("something something")
                .ttlDays(15)
                .maxItems(5)
                .tags(Arrays.asList("uno", "dos"))
                .replicationSource("theSources")
                .storage("whyNotEnum?")
                .protect(false)
                .mutableTime(TimeUtil.now())
                .allowZeroBytes(false)
                .build();
        assertTrue(config.equals(config.toBuilder().build()));

        assertTrue(config.equals(ChannelConfig.createFromJson(config.toJson())));
    }

    @Test
    public void testEnforceChannelRetention() throws IOException {
        ChannelConfig config = ChannelConfig.builder()
                .owner("ABC")
                .description("something something")
                .ttlDays(15)
                .maxItems(5)
                .tags(Arrays.asList("uno", "dos"))
                .replicationSource("theSources")
                .storage("whyNotEnum?")
                .protect(false)
                .allowZeroBytes(false)
                .build();

        ChannelConfig config2 = ChannelConfig.builder()
                .owner("ABC")
                .description("something something")
                .keepForever(true)
                .ttlDays(15)
                .build();
        ChannelConfig updated = ChannelConfig.updateFromJson(config, config2.toJson());
        assertEquals(0, updated.getMaxItems());
        assertEquals(0, updated.getTtlDays());
    }

    @Test
    public void testMutableTime() throws Exception {
        ChannelConfig defaults = ChannelConfig.builder().name("defaults").build();
        DateTime mutableTime = TimeUtil.now();
        ChannelConfig channelConfig = defaults.toBuilder().mutableTime(mutableTime).build();
        assertEquals(mutableTime, channelConfig.getMutableTime());

        String json = channelConfig.toJson();
        ChannelConfig updated = ChannelConfig.updateFromJson(defaults, json);
        assertEquals(mutableTime, updated.getMutableTime());

        ChannelConfig createdFromJson = ChannelConfig.createFromJson(updated.toJson());
        assertEquals(updated, createdFromJson);

    }

    @Test
    public void testZeroBytes() {
        ChannelConfig testZeroBytes = ChannelConfig.builder().name("testZeroBytes").allowZeroBytes(false).build();
        assertFalse(testZeroBytes.isAllowZeroBytes());
    }

}

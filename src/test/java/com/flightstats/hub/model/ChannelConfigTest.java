package com.flightstats.hub.model;

import com.flightstats.hub.exception.InvalidRequestException;
import com.flightstats.hub.util.TimeUtil;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@Slf4j
class ChannelConfigTest {

    @Test
    void testDefaults() {
        ChannelConfig config = ChannelConfig.builder().name("defaults").build();
        assertDefaults(config);
        ChannelConfig copy = config.toBuilder().build();
        assertDefaults(copy);
        assertEquals(config, copy);
    }

    @Test
    void testJsonDefaults() {
        assertDefaults(ChannelConfig.createFromJson("{\"name\": \"defaults\"}"));
    }

    private void assertDefaults(ChannelConfig config) {
        assertEquals("defaults", config.getName());
        assertEquals(120, config.getTtlDays());
        assertEquals("", config.getDescription());
        assertTrue(config.getTags().isEmpty());
        assertEquals("", config.getReplicationSource());
        assertEquals("BATCH", config.getStorage());
        assertNull(config.getMutableTime());
        assertTrue(config.isAllowZeroBytes());
    }

    @Test
    void testDescription() {
        ChannelConfig config = ChannelConfig.builder().description("some thing").build();
        assertEquals("some thing", config.getDescription());
    }

    @Test
    void testDescriptionCopy() {
        ChannelConfig config = ChannelConfig.builder().description("some copy").build();
        ChannelConfig copy = config.toBuilder().build();
        assertEquals("some copy", copy.getDescription());
        assertEquals(config, copy);
    }

    @Test
    void testTags() {
        List<String> tags = Arrays.asList("one", "two", "three", "4 four");
        ChannelConfig config = ChannelConfig.builder().tags(tags).build();
        assertEquals(4, config.getTags().size());
        assertTrue(config.getTags().containsAll(tags));
    }

    @Test
    void testTagsCopy() {
        List<String> tags = Arrays.asList("one", "two", "three", "4 four");
        ChannelConfig config = ChannelConfig.builder().tags(tags).build();
        ChannelConfig copy = config.toBuilder().build();
        assertTrue(copy.getTags().containsAll(config.getTags()));
        assertEquals(config, copy);
    }

    @Test
    void testReplicationSource() {
        String replicationSource = "http://hub/channel/blah";
        ChannelConfig config = ChannelConfig.builder().replicationSource(replicationSource).build();
        assertEquals(replicationSource, config.getReplicationSource());
    }

    @Test
    void testReplicationSourceCopy() {
        String replicationSource = "http://hub/channel/blah";
        ChannelConfig config = ChannelConfig.builder().replicationSource(replicationSource).build();
        ChannelConfig copy = config.toBuilder().build();
        assertEquals(replicationSource, copy.getReplicationSource());
        assertEquals(config, copy);
    }

    @Test
    void testTypeCopy() {
        ChannelConfig config = ChannelConfig.builder().storage("BOTH").build();
        ChannelConfig copy = config.toBuilder().build();
        assertEquals("BOTH", copy.getStorage());
        assertEquals(config, copy);
    }

    @Test
    void testHasChanged() {
        ChannelConfig defaults = ChannelConfig.builder().name("defaults").build();
        ChannelConfig hasCheez = ChannelConfig.builder().name("hasCheez").build();

        ChannelConfig repl = hasCheez.toBuilder().replicationSource("R").build();
        assertNotEquals(repl, defaults);

        ChannelConfig desc = hasCheez.toBuilder().description("D").build();
        assertNotEquals(desc, defaults);

        ChannelConfig max = hasCheez.toBuilder().maxItems(1).build();
        assertNotEquals(max, defaults);

        ChannelConfig owner = hasCheez.toBuilder().owner("O").build();
        assertNotEquals(owner, defaults);

        ChannelConfig storage = hasCheez.toBuilder().storage("S").build();
        assertNotEquals(storage, defaults);

        ChannelConfig ttl = hasCheez.toBuilder().ttlDays(5).build();
        assertNotEquals(ttl, defaults);

        ChannelConfig tags = hasCheez.toBuilder().tags(Sets.newHashSet("one", "two")).build();
        assertNotEquals(tags, defaults);
    }

    @Test
    void testCopy() {
        ChannelConfig config = ChannelConfig.builder()
                .owner("ABC")
                .description("something something")
                .ttlDays(15)
                .maxItems(5)
                .tags(Arrays.asList("uno", "dos"))
                .replicationSource("theSources")
                .storage("SINGLE")
                .protect(false)
                .mutableTime(TimeUtil.now())
                .allowZeroBytes(false)
                .build();

        assertEquals(config, config.toBuilder().build());
        assertEquals(config, ChannelConfig.createFromJson(config.toJson()));
    }

    @Test
    void testEnforceChannelRetention() {
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
    void defaultMutableTimeChannel_singleStorage_equalAttributes() {
        DateTime mutableTime = TimeUtil.now();
        ChannelConfig historical = ChannelConfig.builder()
                .mutableTime(mutableTime)
                .name("historical").build();
        assertEquals(historical.getMutableTime(), mutableTime);
        assertEquals(historical.getStorage(), ChannelType.SINGLE.name());
    }

    @Test
    void updateToMutableTime_updateStorage_true() {
        ChannelConfig defaults = ChannelConfig.builder()
                .storage(ChannelType.SINGLE.name())
                .name("defaults").build();

        DateTime mutableTime = TimeUtil.now();
        ChannelConfig channelConfig = defaults.toBuilder().mutableTime(mutableTime).storage(ChannelType.SINGLE.name()).build();
        assertEquals(mutableTime, channelConfig.getMutableTime());
    }

    @Test
    void mutableTimeFailOnUpdate_noUpdateDefaultStorage_throws() {
        ChannelConfig single = ChannelConfig.builder()
                .name("defaults").build();
        DateTime mutableTime = TimeUtil.now();
        assertThrows(InvalidRequestException.class, () -> single.toBuilder().mutableTime(mutableTime).build());
    }

    @Test
    void updateMutableTime_customSingleStorageOnOriginalChannelCreate_equalConfigs() {
        ChannelConfig single = ChannelConfig.builder()
                .name("single").build();
        DateTime mutableTime = TimeUtil.now();
        ChannelConfig channelConfig = single.toBuilder()
                .storage(ChannelType.SINGLE.name())
                .mutableTime(mutableTime)
                .build();
        assertEquals(mutableTime, channelConfig.getMutableTime());

        String json = channelConfig.toJson();
        ChannelConfig updated = ChannelConfig.updateFromJson(single, json);
        assertEquals(mutableTime, updated.getMutableTime());

        ChannelConfig createdFromJson = ChannelConfig.createFromJson(updated.toJson());
        assertEquals(updated, createdFromJson);

    }

    @Test
    void testZeroBytes() {
        ChannelConfig testZeroBytes = ChannelConfig.builder().name("testZeroBytes").allowZeroBytes(false).build();
        assertFalse(testZeroBytes.isAllowZeroBytes());
    }

    @Test
    void testSecondaryMetricsReporting_defaultEmpty() {
        ChannelConfig testSecondaryMetricsNull = ChannelConfig
                .builder()
                .name("testSecondaryMetrics0")
                .build();
        assertFalse(testSecondaryMetricsNull.isSecondaryMetricsReporting());
    }

    @Test
    void testSecondaryMetricsReporting_built() {
        ChannelConfig testSecondaryMetrics = ChannelConfig
                .builder()
                .name("testSecondaryMetrics1")
                .secondaryMetricsReporting(true)
                .build();
        assertTrue(testSecondaryMetrics.isSecondaryMetricsReporting());
    }

    @Test
    void testSecondaryMetricsReportingUpdateFromJson_updated() {
        ChannelConfig testSecondaryMetrics = ChannelConfig
                .builder()
                .name("testSecondaryMetrics2")
                .build();
        String update = "{ \"secondaryMetricsReporting\": \"true\" }";
        ChannelConfig updated = ChannelConfig.updateFromJson(testSecondaryMetrics, update);
        assertTrue(updated.isSecondaryMetricsReporting());
    }

}

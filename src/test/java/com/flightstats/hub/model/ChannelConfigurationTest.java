package com.flightstats.hub.model;

import org.junit.Test;

import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.TimeUnit;

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
        assertTrue(config.isSequence());
        assertEquals(1L, config.getContentThroughputInSeconds());
        assertEquals(1L, config.getPeakRequestRateSeconds());
        assertEquals("", config.getDescription());
        assertTrue(config.getTags().isEmpty());
    }

    @Test
    public void testTimeSequence() throws Exception {
        Date date = new Date(123456L);
        ChannelConfiguration config = ChannelConfiguration.builder()
                .withName("options")
                .withTtlDays(10L)
                .withType(ChannelConfiguration.ChannelType.TimeSeries)
                .withCreationDate(date)
                .withContentKiloBytes(100)
                .build();
        assertOptions(date, config, false);
        ChannelConfiguration copy = ChannelConfiguration.builder().withChannelConfiguration(config).build();
        assertOptions(date, copy, false);
    }

    private void assertOptions(Date date, ChannelConfiguration config, boolean isSequence) {
        assertEquals("options", config.getName());
        assertEquals( 10L, config.getTtlDays());
        assertEquals(isSequence, config.isSequence());
        assertEquals(date, config.getCreationDate());
        assertEquals(1L, config.getPeakRequestRateSeconds());
        assertEquals(100L, config.getContentThroughputInSeconds());
    }

    @Test
    public void testMillis() throws Exception {
        ChannelConfiguration config = ChannelConfiguration.builder().withName("millis100").withTtlMillis(100L).build();
        assertEquals(100L, (long)config.getTtlMillis());
        assertEquals(1, config.getTtlDays());
    }

    @Test
    public void testMillisOneDay() throws Exception {
        ChannelConfiguration config = ChannelConfiguration.builder().withName("millisOne").withTtlMillis(TimeUnit.DAYS.toMillis(1)).build();
        assertEquals(86400000L, (long)config.getTtlMillis());
        assertEquals(1, config.getTtlDays());
    }

    @Test
    public void testMillisTwoDays() throws Exception {
        ChannelConfiguration config = ChannelConfiguration.builder().withName("millis").withTtlMillis(TimeUnit.DAYS.toMillis(1) + 10).build();
        assertEquals(86400010L, (long)config.getTtlMillis());
        assertEquals(2, config.getTtlDays());
    }

    @Test
    public void testMillisNull() throws Exception {
        ChannelConfiguration config = ChannelConfiguration.builder().withName("millisNull").withTtlMillis(null).build();
        assertEquals(31536000000000L, (long)config.getTtlMillis());
        assertEquals(1000 * 365, config.getTtlDays());
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
        //todo - gfm - 4/28/14 - add some tags to a config
        ChannelConfiguration config = ChannelConfiguration.builder().withTags("one,two , three, 4 four").build();
        assertEquals(4, config.getTags().size());
        assertTrue(config.getTags().containsAll(Arrays.asList("one", "two", "three", "4 four")));
    }

    @Test
    public void testTagsCopy() throws Exception {
        ChannelConfiguration config = ChannelConfiguration.builder().withTags("one,two , three, 4 four").build();
        ChannelConfiguration copy = ChannelConfiguration.builder().withChannelConfiguration(config).build();
        assertTrue(copy.getTags().containsAll(config.getTags()));
    }
}

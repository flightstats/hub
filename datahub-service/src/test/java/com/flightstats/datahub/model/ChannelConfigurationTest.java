package com.flightstats.datahub.model;

import org.junit.Test;

import java.util.Date;

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

    private void assertDefaults(ChannelConfiguration config) {
        assertEquals("defaults", config.getName());
        assertEquals(10368000000L, config.getTtlMillis().longValue());
        assertTrue(config.isSequence());
        assertEquals(100L, config.getContentThroughputInSeconds());
        assertEquals(10L, config.getPeakRequestRateSeconds());
    }

    @Test
    public void testTimeSequence() throws Exception {
        Date date = new Date(123456L);
        ChannelConfiguration config = ChannelConfiguration.builder()
                .withName("options")
                .withTtlMillis(10L)
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
        assertEquals((Long) 10L, config.getTtlMillis());
        assertEquals(isSequence, config.isSequence());
        assertEquals(date, config.getCreationDate());
        assertEquals(10L, config.getPeakRequestRateSeconds());
        assertEquals(1000L, config.getContentThroughputInSeconds());
    }
}

package com.flightstats.datahub.model;

import org.junit.Test;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

/**
 *
 */
public class ChannelConfigurationTest {

    @Test
    public void testDefaults() throws Exception {
        ChannelConfiguration config = ChannelConfiguration.builder().withName("defaults").build();
        assertEquals("defaults", config.getName());
        assertNull(config.getTtlMillis());
        assertTrue(config.isSequence());
        assertEquals(10L, config.getContentThroughputInSeconds());
        assertEquals(1L, config.getRequestRateInSeconds());
    }

    @Test
    public void testOptions() throws Exception {
        Date date = new Date(123456L);
        ChannelConfiguration config = ChannelConfiguration.builder()
                .withName("options")
                .withTtlMillis(10L)
                .withType(ChannelConfiguration.ChannelType.TimeSeries)
                .withCreationDate(date)
                .withContentKiloBytes(100)
                .withPeakRequestRate(100, TimeUnit.MINUTES)
                .build();
        assertOptions(date, config);
        ChannelConfiguration copy = ChannelConfiguration.builder().withChannelConfiguration(config).build();
        assertOptions(date, copy);

    }

    private void assertOptions(Date date, ChannelConfiguration config) {
        assertEquals("options", config.getName());
        assertEquals((Long) 10L, config.getTtlMillis());
        assertFalse(config.isSequence());
        assertEquals(date, config.getCreationDate());
        assertEquals(2L, config.getRequestRateInSeconds());
        assertEquals(200L, config.getContentThroughputInSeconds());
    }
}

package com.flightstats.hub.metrics;

import org.junit.Test;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;



public class MetricsConfigTest {

    @Test
    public void testDefaultValues_defaultTags() {
        MetricsConfig metricsConfig = MetricsConfig
                .builder()
                .buildWithDefaults()
                .build();
        assertEquals("hub", metricsConfig.getRole());
        assertEquals("development", metricsConfig.getTeam());
        assertEquals("dev", metricsConfig.getEnv());
        assertEquals("local", metricsConfig.getAppVersion());
        assertEquals("http", metricsConfig.getInfluxdbProtocol());
        assertEquals("localhost", metricsConfig.getInfluxdbHost());
        assertEquals(8086, metricsConfig.getInfluxdbPort());
        assertEquals("", metricsConfig.getInfluxdbPass());
        assertEquals("", metricsConfig.getInfluxdbUser());
        assertEquals("hub_tick", metricsConfig.getInfluxdbDatabaseName());
        assertEquals("local-dev", metricsConfig.getClusterTag());
        assertEquals(15, metricsConfig.getReportingIntervalSeconds());
        assertFalse(metricsConfig.isEnabled());
    }

}

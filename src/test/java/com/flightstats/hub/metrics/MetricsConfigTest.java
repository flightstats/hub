package com.flightstats.hub.metrics;

import com.flightstats.hub.app.HubBindings;
import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.app.HubPropertiesTest;
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

        String clusterTag = HubProperties.getProperty("cluster.location", "local") +
                "-" +
                HubProperties.getProperty("app.environment", "dev");

        assertEquals(HubProperties.getProperty("metrics.tag.role", "hub"), metricsConfig.getRole());
        assertEquals(HubProperties.getProperty("metrics.tag.team", "development"), metricsConfig.getTeam());
        assertEquals(HubProperties.getProperty("app.environment", "dev"), metricsConfig.getEnv());
        assertEquals("local", metricsConfig.getAppVersion());
        assertEquals(HubProperties.getProperty("metrics.influxdb.protocol", "http"), metricsConfig.getInfluxdbProtocol());
        assertEquals(HubProperties.getProperty("metrics.influxdb.host", "localhost"), metricsConfig.getInfluxdbHost());
        assertEquals(HubProperties.getProperty("metrics.influxdb.port", 8086), metricsConfig.getInfluxdbPort());
        assertEquals(HubProperties.getProperty("metrics.influxdb.user", ""), metricsConfig.getInfluxdbPass());
        assertEquals(HubProperties.getProperty("metrics.influxdb.password", ""), metricsConfig.getInfluxdbUser());
        assertEquals(HubProperties.getProperty("metrics.influxdb.database.name", "hub_tick"), metricsConfig.getInfluxdbDatabaseName());
        assertEquals(clusterTag, metricsConfig.getClusterTag());
        assertEquals(HubProperties.getProperty("metrics.seconds", 15), metricsConfig.getReportingIntervalSeconds());
        assertFalse(metricsConfig.isEnabled());
    }

}

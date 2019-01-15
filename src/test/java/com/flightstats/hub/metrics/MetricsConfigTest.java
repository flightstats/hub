package com.flightstats.hub.metrics;

import com.flightstats.hub.app.HubHost;
import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.app.HubVersion;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;



public class MetricsConfigTest {
    @Test
    public void testExpectedValues_customBuiltTags() {
        HubVersion hubVersion = mock(HubVersion.class);
        when(hubVersion.getVersion()).thenReturn("local");

        MetricsConfig metricsConfig = MetricsConfig.builder()
            .appVersion(hubVersion.getVersion())
            .clusterTag("location-test")
            .env("test")
            .enabled(false)
            .hostTag(HubHost.getLocalName())
            .influxdbDatabaseName("hub_test")
            .influxdbHost("influxdb")
            .influxdbPass("")
            .influxdbPort(8086)
            .influxdbProtocol("https")
            .influxdbUser("")
            .reportingIntervalSeconds(15)
            .role("hub")
            .team("testers")
            .build();

        assertEquals("hub", metricsConfig.getRole());
        assertEquals("testers", metricsConfig.getTeam());
        assertEquals("test", metricsConfig.getEnv());
        assertEquals("local", metricsConfig.getAppVersion());
        assertEquals("https", metricsConfig.getInfluxdbProtocol());
        assertEquals("influxdb", metricsConfig.getInfluxdbHost());
        assertEquals(8086, metricsConfig.getInfluxdbPort());
        assertEquals( "", metricsConfig.getInfluxdbPass());
        assertEquals( "", metricsConfig.getInfluxdbUser());
        assertEquals("hub_test", metricsConfig.getInfluxdbDatabaseName());
        assertEquals("location-test", metricsConfig.getClusterTag());
        assertEquals( 15, metricsConfig.getReportingIntervalSeconds());
        assertFalse(metricsConfig.isEnabled());
    }

}

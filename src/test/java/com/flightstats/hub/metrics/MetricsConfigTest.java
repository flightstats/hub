package com.flightstats.hub.metrics;

import org.junit.Test;
import java.util.Properties;
import static org.testng.AssertJUnit.*;
import static com.flightstats.hub.app.HubHost.getLocalName;


public class MetricsConfigTest {
    @Test
    public void testDefaultValues_defaultTags() {
        Properties emptyProperties = new Properties();
        MetricsConfig metricsConfig = new MetricsConfig(emptyProperties);
        assertEquals(metricsConfig.getRole(), "");
        assertEquals(metricsConfig.getTeam(), "");
        assertEquals(metricsConfig.getEnv(), "dev");
        assertEquals(metricsConfig.getClusterLocation(), "local");
        assertEquals(metricsConfig.getAppVersion(), "local");
        assertEquals(metricsConfig.getInfluxdbProtocol(), "http");
        assertEquals(metricsConfig.getInfluxdbHost(), "localhost");
        assertEquals(metricsConfig.getInfluxdbPort(), 8086);
        assertEquals(metricsConfig.getInfluxdbPass(), "");
        assertEquals(metricsConfig.getInfluxdbUser(), "");
        assertEquals(metricsConfig.getInfluxdbDatabaseName(), "hubmain");
        assertEquals(metricsConfig.getClusterTag(), "local-dev");
        assertEquals(metricsConfig.getReportingIntervalSeconds(),  15);
        assertFalse(metricsConfig.enabled());
    }

    @Test
    public void testDefaultValuesAfterFailedFileLoad_defaultTags() {
        Properties emptyProperties = new Properties();
        MetricsConfig metricsConfig = new MetricsConfig(emptyProperties);
        boolean loaded = metricsConfig.loadValues("/not/a/path");
        assertFalse(loaded);
        assertEquals(metricsConfig.getRole(), "");
        assertEquals(metricsConfig.getTeam(), "");
        assertEquals(metricsConfig.getEnv(), "dev");
        assertEquals(metricsConfig.getClusterLocation(), "local");
        assertEquals(metricsConfig.getAppVersion(), "local");
        assertEquals(metricsConfig.getInfluxdbProtocol(), "http");
        assertEquals(metricsConfig.getInfluxdbHost(), "localhost");
        assertEquals(metricsConfig.getInfluxdbPort(), 8086);
        assertEquals(metricsConfig.getInfluxdbPass(), "");
        assertEquals(metricsConfig.getInfluxdbUser(), "");
        assertEquals(metricsConfig.getInfluxdbDatabaseName(), "hubmain");
        assertEquals(metricsConfig.getClusterTag(), "local-dev");
        assertEquals(metricsConfig.getReportingIntervalSeconds(),  15);
        assertFalse(metricsConfig.enabled());
    }

    @Test
    public void testCustomValues_envTag() {
        Properties customProperties = new Properties();
        customProperties.setProperty("app.environment", "staging");
        MetricsConfig metricsConfig = new MetricsConfig(customProperties);
        assertEquals("staging", metricsConfig.getEnv());
    }

    @Test
    public void testCustomValues_clusterLocationTag() {
        Properties customProperties = new Properties();
        customProperties.setProperty("cluster.location", "dls");
        MetricsConfig metricsConfig = new MetricsConfig(customProperties);
        assertEquals("dls", metricsConfig.getClusterLocation());
    }

    @Test
    public void testCustomValues_influxdbProtocolTag() {
        Properties customProperties = new Properties();
        customProperties.setProperty("metrics.influxdb.protocol", "https");
        MetricsConfig metricsConfig = new MetricsConfig(customProperties);
        assertEquals("https", metricsConfig.getInfluxdbProtocol());
    }

    @Test
    public void testCustomValues_influxdbHostTag() {
        Properties customProperties = new Properties();
        customProperties.setProperty("metrics.influxdb.host", "hub.test.io");
        MetricsConfig metricsConfig = new MetricsConfig(customProperties);
        assertEquals("hub.test.io", metricsConfig.getInfluxdbHost());
    }

    @Test
    public void testCustomValues_influxdbPortTag() {
        Properties customProperties = new Properties();
        customProperties.setProperty("metrics.influxdb.port", "9999");
        MetricsConfig metricsConfig = new MetricsConfig(customProperties);
        assertEquals(9999, metricsConfig.getInfluxdbPort());
    }

    @Test
    public void testCustomValues_influxdbUserTag() {
        Properties customProperties = new Properties();
        customProperties.setProperty("metrics.influxdb.database.user", "test_user");
        MetricsConfig metricsConfig = new MetricsConfig(customProperties);
        assertEquals("test_user", metricsConfig.getInfluxdbUser());
    }

    @Test
    public void testCustomValues_influxdbPasswordTag() {
        Properties customProperties = new Properties();
        customProperties.setProperty("metrics.influxdb.database.password", "$$$$$$");
        MetricsConfig metricsConfig = new MetricsConfig(customProperties);
        assertEquals("$$$$$$", metricsConfig.getInfluxdbPass());
    }

    @Test
    public void testCustomValues_influxdbDatabaseNameTag() {
        Properties customProperties = new Properties();
        customProperties.setProperty("metrics.influxdb.database.name", "hub_test");
        MetricsConfig metricsConfig = new MetricsConfig(customProperties);
        assertEquals("hub_test", metricsConfig.getInfluxdbDatabaseName());
    }

    @Test
    public void testCustomValues_metricsEnabled() {
        Properties customProperties = new Properties();
        customProperties.setProperty("metrics.enable", "true");
        MetricsConfig metricsConfig = new MetricsConfig(customProperties);
        assertTrue(metricsConfig.enabled());
    }

    @Test
    public void testCustomValues_clusterNameTag() {
        Properties customProperties = new Properties();
        customProperties.setProperty("app.environment", "test");
        customProperties.setProperty("cluster.location", "dls");
        MetricsConfig metricsConfig = new MetricsConfig(customProperties);
        assertEquals("dls-test", metricsConfig.getClusterTag());
    }

    @Test
    public void testCustomValues_reporterInterval() {
        Properties customProperties = new Properties();
        customProperties.setProperty("metrics.seconds", "30");
        MetricsConfig metricsConfig = new MetricsConfig(customProperties);
        assertEquals(30, metricsConfig.getReportingIntervalSeconds());
    }

    @Test
    public void testGetHostTag_HubHostGetLocalName() {
        Properties customProperties = new Properties();
        MetricsConfig metricsConfig = new MetricsConfig(customProperties);
        assertEquals(getLocalName(), metricsConfig.getHostTag());
    }
}

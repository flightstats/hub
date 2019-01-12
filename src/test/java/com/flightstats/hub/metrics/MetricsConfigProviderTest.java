package com.flightstats.hub.metrics;

import org.junit.Test;
import java.util.Properties;
import static org.testng.AssertJUnit.*;
import static com.flightstats.hub.app.HubHost.getLocalName;


public class MetricsConfigProviderTest {
    @Test
    public void testDefaultValues_defaultTags() {
        Properties emptyProperties = new Properties();
        MetricsConfigProvider metricsConfigProvider = new MetricsConfigProvider(emptyProperties);
        assertEquals(metricsConfigProvider.getRole(), "hub");
        assertEquals(metricsConfigProvider.getTeam(), "ddt");
        assertEquals(metricsConfigProvider.getEnv(), "dev");
        assertEquals(metricsConfigProvider.getClusterLocation(), "local");
        assertEquals(metricsConfigProvider.getAppVersion(), "local");
        assertEquals(metricsConfigProvider.getInfluxdbProtocol(), "http");
        assertEquals(metricsConfigProvider.getInfluxdbHost(), "localhost");
        assertEquals(metricsConfigProvider.getInfluxdbPort(), 8086);
        assertEquals(metricsConfigProvider.getInfluxdbPass(), "");
        assertEquals(metricsConfigProvider.getInfluxdbUser(), "");
        assertEquals(metricsConfigProvider.getInfluxdbDatabaseName(), "hub_main");
        assertEquals(metricsConfigProvider.getClusterTag(), "local-dev");
        assertEquals(metricsConfigProvider.getReportingIntervalSeconds(),  15);
        assertFalse(metricsConfigProvider.enabled());
    }

    @Test
    public void testDefaultValuesAfterFailedFileLoad_defaultTags() {
        Properties emptyProperties = new Properties();
        MetricsConfigProvider metricsConfigProvider = new MetricsConfigProvider(emptyProperties);
        boolean loaded = metricsConfigProvider.loadValues("/not/a/path");
        assertFalse(loaded);
        assertEquals(metricsConfigProvider.getRole(), "hub");
        assertEquals(metricsConfigProvider.getTeam(), "ddt");
        assertEquals(metricsConfigProvider.getEnv(), "dev");
        assertEquals(metricsConfigProvider.getClusterLocation(), "local");
        assertEquals(metricsConfigProvider.getAppVersion(), "local");
        assertEquals(metricsConfigProvider.getInfluxdbProtocol(), "http");
        assertEquals(metricsConfigProvider.getInfluxdbHost(), "localhost");
        assertEquals(metricsConfigProvider.getInfluxdbPort(), 8086);
        assertEquals(metricsConfigProvider.getInfluxdbPass(), "");
        assertEquals(metricsConfigProvider.getInfluxdbUser(), "");
        assertEquals(metricsConfigProvider.getInfluxdbDatabaseName(), "hub_main");
        assertEquals(metricsConfigProvider.getClusterTag(), "local-dev");
        assertEquals(metricsConfigProvider.getReportingIntervalSeconds(),  15);
        assertFalse(metricsConfigProvider.enabled());
    }

    @Test
    public void testCustomValues_envTag() {
        Properties customProperties = new Properties();
        customProperties.setProperty("app.environment", "staging");
        MetricsConfigProvider metricsConfigProvider = new MetricsConfigProvider(customProperties);
        assertEquals("staging", metricsConfigProvider.getEnv());
    }

    @Test
    public void testCustomValues_clusterLocationTag() {
        Properties customProperties = new Properties();
        customProperties.setProperty("cluster.location", "dls");
        MetricsConfigProvider metricsConfigProvider = new MetricsConfigProvider(customProperties);
        assertEquals("dls", metricsConfigProvider.getClusterLocation());
    }

    @Test
    public void testCustomValues_influxdbProtocolTag() {
        Properties customProperties = new Properties();
        customProperties.setProperty("metrics.influxdb.protocol", "https");
        MetricsConfigProvider metricsConfigProvider = new MetricsConfigProvider(customProperties);
        assertEquals("https", metricsConfigProvider.getInfluxdbProtocol());
    }

    @Test
    public void testCustomValues_influxdbHostTag() {
        Properties customProperties = new Properties();
        customProperties.setProperty("metrics.influxdb.host", "hub.test.io");
        MetricsConfigProvider metricsConfigProvider = new MetricsConfigProvider(customProperties);
        assertEquals("hub.test.io", metricsConfigProvider.getInfluxdbHost());
    }

    @Test
    public void testCustomValues_influxdbPortTag() {
        Properties customProperties = new Properties();
        customProperties.setProperty("metrics.influxdb.port", "9999");
        MetricsConfigProvider metricsConfigProvider = new MetricsConfigProvider(customProperties);
        assertEquals(9999, metricsConfigProvider.getInfluxdbPort());
    }

    @Test
    public void testCustomValues_influxdbUserTag() {
        Properties customProperties = new Properties();
        customProperties.setProperty("metrics.influxdb.database.user", "test_user");
        MetricsConfigProvider metricsConfigProvider = new MetricsConfigProvider(customProperties);
        assertEquals("test_user", metricsConfigProvider.getInfluxdbUser());
    }

    @Test
    public void testCustomValues_influxdbPasswordTag() {
        Properties customProperties = new Properties();
        customProperties.setProperty("metrics.influxdb.database.password", "$$$$$$");
        MetricsConfigProvider metricsConfigProvider = new MetricsConfigProvider(customProperties);
        assertEquals("$$$$$$", metricsConfigProvider.getInfluxdbPass());
    }

    @Test
    public void testCustomValues_influxdbDatabaseNameTag() {
        Properties customProperties = new Properties();
        customProperties.setProperty("metrics.influxdb.database.name", "hub_test");
        MetricsConfigProvider metricsConfigProvider = new MetricsConfigProvider(customProperties);
        assertEquals("hub_test", metricsConfigProvider.getInfluxdbDatabaseName());
    }

    @Test
    public void testCustomValues_metricsEnabled() {
        Properties customProperties = new Properties();
        customProperties.setProperty("metrics.enabled", "true");
        MetricsConfigProvider metricsConfigProvider = new MetricsConfigProvider(customProperties);
        assertTrue(metricsConfigProvider.enabled());
    }

    @Test
    public void testCustomValues_clusterNameTag() {
        Properties customProperties = new Properties();
        customProperties.setProperty("app.environment", "test");
        customProperties.setProperty("cluster.location", "dls");
        MetricsConfigProvider metricsConfigProvider = new MetricsConfigProvider(customProperties);
        assertEquals("dls-test", metricsConfigProvider.getClusterTag());
    }

    @Test
    public void testCustomValues_reporterInterval() {
        Properties customProperties = new Properties();
        customProperties.setProperty("metrics.seconds", "30");
        MetricsConfigProvider metricsConfigProvider = new MetricsConfigProvider(customProperties);
        assertEquals(30, metricsConfigProvider.getReportingIntervalSeconds());
    }

    @Test
    public void testGetHostTag_HubHostGetLocalName() {
        Properties customProperties = new Properties();
        MetricsConfigProvider metricsConfigProvider = new MetricsConfigProvider(customProperties);
        assertEquals(getLocalName(), metricsConfigProvider.getHostTag());
    }
}

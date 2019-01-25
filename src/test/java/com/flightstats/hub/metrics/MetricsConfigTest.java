package com.flightstats.hub.metrics;

import org.junit.Test;
import java.util.Properties;
import static org.testng.AssertJUnit.*;
import static com.flightstats.hub.app.HubHost.getLocalName;


public class MetricsConfigTest {

    @Test
    public void testDefaultValues_defaultTags() {
        MetricsConfig metricsConfig = MetricsConfig
                .builder()
                .build();
        assertEquals(metricsConfig.getRole(), "");
        assertEquals(metricsConfig.getTeam(), "");
        assertEquals(metricsConfig.getEnv(), "dev");
        assertEquals(metricsConfig.getAppVersion(), "local");
        assertEquals(metricsConfig.getInfluxdbProtocol(), "http");
        assertEquals(metricsConfig.getInfluxdbHost(), "localhost");
        assertEquals(metricsConfig.getInfluxdbPort(), 8086);
        assertEquals(metricsConfig.getInfluxdbPass(), "");
        assertEquals(metricsConfig.getInfluxdbUser(), "");
        assertEquals(metricsConfig.getInfluxdbDatabaseName(), "hubmain");
        assertEquals(metricsConfig.getClusterTag(), "local-dev");
        assertEquals(metricsConfig.getReportingIntervalSeconds(),  15);
        assertFalse(metricsConfig.isEnabled());
    }

//    @Test
//    public void testCustomValues_customValues() {
//        Properties customProperties = new Properties();
//        customProperties.setProperty("app.environment", "staging");
//        customProperties.setProperty("metrics.influxdb.protocol", "https");
//        customProperties.setProperty("metrics.influxdb.host", "hub.test.io");
//        customProperties.setProperty("metrics.influxdb.port", "9999");
//        customProperties.setProperty("metrics.enable", "true");
//        customProperties.setProperty("metrics.influxdb.database.user", "test_user");
//        customProperties.setProperty("metrics.influxdb.database.password", "$$$$$$");
//        customProperties.setProperty("metrics.influxdb.database.name", "hub_test");
//        customProperties.setProperty("cluster.location", "dls");
//        customProperties.setProperty("metrics.seconds", "30");
//        MetricsConfig metricsConfigCustom = MetricsConfig
//                .builder()
//                .properties(customProperties)
//                .build();
//
//        assertEquals("staging", metricsConfigCustom.getEnv());
//        assertEquals("https", metricsConfigCustom.getInfluxdbProtocol());
//        assertEquals("hub.test.io", metricsConfigCustom.getInfluxdbHost());
//        assertEquals(9999, metricsConfigCustom.getInfluxdbPort());
//        assertEquals("test_user", metricsConfigCustom.getInfluxdbUser());
//        assertEquals("$$$$$$", metricsConfigCustom.getInfluxdbPass());
//        assertEquals("hub_test", metricsConfigCustom.getInfluxdbDatabaseName());
//        assertTrue(metricsConfigCustom.isEnabled());
//        assertEquals("dls-staging", metricsConfigCustom.getClusterTag());
//        assertEquals(30, metricsConfigCustom.getReportingIntervalSeconds());
//        assertEquals(getLocalName(), metricsConfigCustom.getHostTag());
//    }
//
//    @Test
//    public void testCustomValuesByIndividualBuilders_customValues() {
//        Properties customProperties = new Properties();
//        customProperties.setProperty("app.environment", "testing");
//        customProperties.setProperty("metrics.influxdb.protocol", "https");
//        customProperties.setProperty("metrics.influxdb.host", "hub.test.io");
//        customProperties.setProperty("metrics.influxdb.port", "9999");
//        customProperties.setProperty("metrics.enable", "true");
//        customProperties.setProperty("metrics.influxdb.database.user", "test_user");
//        customProperties.setProperty("metrics.influxdb.database.password", "$$$$$$");
//        customProperties.setProperty("metrics.influxdb.database.name", "hub_test");
//        customProperties.setProperty("cluster.location", "dls");
//        customProperties.setProperty("metrics.seconds", "30");
//        customProperties.setProperty("metrics.enabled", "false");
//        MetricsConfig metricsConfigCustom = MetricsConfig
//                .builder()
//                .properties(customProperties)
//                .env("testing")
//                .influxdbProtocol("udp")
//                .influxdbHost("hub.test2.io")
//                .influxdbPort(8765)
//                .influxdbUser("test_user2")
//                .influxdbPass("@@@@@@")
//                .influxdbDatabaseName("hub_test2")
//                .clusterTag("dls-testing")
//                .reportingIntervalSeconds(31)
//                .hostTag(getLocalName() + "test")
//                .enabled(true)
//                .build();
//
//
//        assertEquals("testing", metricsConfigCustom.getEnv());
//        assertEquals("udp", metricsConfigCustom.getInfluxdbProtocol());
//        assertEquals("hub.test2.io", metricsConfigCustom.getInfluxdbHost());
//        assertEquals(8765, metricsConfigCustom.getInfluxdbPort());
//        assertEquals("test_user2", metricsConfigCustom.getInfluxdbUser());
//        assertEquals("@@@@@@", metricsConfigCustom.getInfluxdbPass());
//        assertEquals("hub_test2", metricsConfigCustom.getInfluxdbDatabaseName());
//        assertTrue(metricsConfigCustom.isEnabled());
//        assertEquals("dls-testing", metricsConfigCustom.getClusterTag());
//        assertEquals(31, metricsConfigCustom.getReportingIntervalSeconds());
//        assertEquals(getLocalName() + "test", metricsConfigCustom.getHostTag());
//    }
//
//    @Test
//    public void testCustomValuesByPropertiesBuilder_overriddenPropertiesValues() {
//        Properties customProperties = new Properties();
//        customProperties.setProperty("app.environment", "testing");
//        customProperties.setProperty("metrics.influxdb.protocol", "https");
//        customProperties.setProperty("metrics.influxdb.host", "hub.test.io");
//        customProperties.setProperty("metrics.influxdb.port", "9999");
//        customProperties.setProperty("metrics.enable", "true");
//        customProperties.setProperty("metrics.influxdb.database.user", "test_user");
//        customProperties.setProperty("metrics.influxdb.database.password", "$$$$$$");
//        customProperties.setProperty("metrics.influxdb.database.name", "hub_test");
//        customProperties.setProperty("cluster.location", "dls");
//        customProperties.setProperty("metrics.seconds", "30");
//        customProperties.setProperty("metrics.enabled", "false");
//        MetricsConfig metricsConfigCustom = MetricsConfig
//                .builder()
//                .env("testing")
//                .influxdbProtocol("udp")
//                .influxdbHost("hub.test2.io")
//                .influxdbPort(8765)
//                .influxdbUser("test_user2")
//                .influxdbPass("@@@@@@")
//                .influxdbDatabaseName("hub_test2")
//                .clusterTag("dls-testing")
//                .reportingIntervalSeconds(31)
//                .hostTag(getLocalName() + "test")
//                .enabled(true)
//                // note the properties file is overridden despite order of ops here
//                .properties(customProperties)
//                .build();
//
//
//        assertEquals("testing", metricsConfigCustom.getEnv());
//        assertEquals("udp", metricsConfigCustom.getInfluxdbProtocol());
//        assertEquals("hub.test2.io", metricsConfigCustom.getInfluxdbHost());
//        assertEquals(8765, metricsConfigCustom.getInfluxdbPort());
//        assertEquals("test_user2", metricsConfigCustom.getInfluxdbUser());
//        assertEquals("@@@@@@", metricsConfigCustom.getInfluxdbPass());
//        assertEquals("hub_test2", metricsConfigCustom.getInfluxdbDatabaseName());
//        assertTrue(metricsConfigCustom.isEnabled());
//        assertEquals("dls-testing", metricsConfigCustom.getClusterTag());
//        assertEquals(31, metricsConfigCustom.getReportingIntervalSeconds());
//        assertEquals(getLocalName() + "test", metricsConfigCustom.getHostTag());
//    }
//
//    @Test
//    public void testDefaultValuesAfterFailedFileLoad_defaultTags() {
//        MetricsConfig metricsConfig = MetricsConfig
//                .builder()
//                .properties("/not/a/valid/path")
//                .build();
//        assertEquals(metricsConfig.getRole(), "");
//        assertEquals(metricsConfig.getTeam(), "");
//        assertEquals(metricsConfig.getEnv(), "dev");
//        assertEquals(metricsConfig.getAppVersion(), "local");
//        assertEquals(metricsConfig.getInfluxdbProtocol(), "http");
//        assertEquals(metricsConfig.getInfluxdbHost(), "localhost");
//        assertEquals(metricsConfig.getInfluxdbPort(), 8086);
//        assertEquals(metricsConfig.getInfluxdbPass(), "");
//        assertEquals(metricsConfig.getInfluxdbUser(), "");
//        assertEquals(metricsConfig.getInfluxdbDatabaseName(), "hubmain");
//        assertEquals(metricsConfig.getClusterTag(), "local-dev");
//        assertEquals(metricsConfig.getReportingIntervalSeconds(),  15);
//        assertFalse(metricsConfig.isEnabled());
//    }

}

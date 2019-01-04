package com.flightstats.hub.metrics;

import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.app.HubVersion;
import metrics_influxdb.HttpInfluxdbProtocol;
import metrics_influxdb.InfluxdbProtocol;
import com.codahale.metrics.MetricRegistry;
import java.util.SortedSet;
import static com.flightstats.hub.app.HubHost.getLocalName;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringContains.containsString;
import static org.testng.AssertJUnit.assertTrue;

public class JVMMetricsTest {
    private static JVMMetricsService metricsService;
    private static final HubVersion hubVersion = new HubVersion();

    @BeforeClass
    public static void before() {
        HubProperties.setProperty("metrics.enable", "true");
        MetricRegistry registry = new MetricRegistry();
        metricsService = new JVMMetricsService(registry, hubVersion);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void registeredMetricNamesTest() {

        SortedSet<String> metricNames = metricsService.getRegisteredMetricNames();
        for (String metricName : metricNames) {
            //noinspection unchecked
            assertThat(metricName, anyOf(containsString("gc"), containsString("memory"), containsString("thread")));
            assertThat(metricName, containsString("jvm_"));
        }
    }

    @Test
    public void getProtocolTest() {
        assertThat(metricsService.getProtocol(), is(InfluxdbProtocol.class));
        assertThat(metricsService.getProtocol(), is(HttpInfluxdbProtocol.class));
    }

    @Test
    public void getHostTest() {
        assertTrue(metricsService.getHost().equalsIgnoreCase(getLocalName()));
    }

    @Test
    public void getVersionTest() {
        assertTrue(metricsService.getVersion().equalsIgnoreCase(hubVersion.getVersion()));
    }

    @AfterClass public static void after() {
        metricsService.stop();
    }

}

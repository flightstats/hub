package com.flightstats.hub.metrics;

import metrics_influxdb.HttpInfluxdbProtocol;
import metrics_influxdb.InfluxdbProtocol;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Iterator;
import java.util.SortedSet;
import com.codahale.metrics.MetricRegistry;
import com.flightstats.hub.app.HubVersion;


import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringContains.containsString;
import static com.flightstats.hub.app.HubHost.getLocalName;

public class JVMMetricsTest {
    private static JVMMetricsService metricsService;
    private static final HubVersion hubVersion = new HubVersion();

    @BeforeClass
    public static void before() {
        System.setProperty("metrics.environment", "test");
        MetricRegistry registry = new MetricRegistry();
        metricsService = new JVMMetricsService(registry, hubVersion);
        metricsService.start();
    }

    @Test
    public void registeredMetricNamesTest() {

        SortedSet<String> metricNames = metricsService.getRegisteredMetricNames();
        Iterator iterator = metricNames.iterator();
        while (iterator.hasNext()) {
            String nextString = (String) iterator.next();
            assertThat(nextString, anyOf(containsString("gc"), containsString("memory"), containsString("thread")));
            assertThat(nextString, containsString("jvm_"));
        }
    }

    @Test
    public void getProtocolTest() {
        assertThat(metricsService.getProtocol(), is(InfluxdbProtocol.class));
        assertThat(metricsService.getProtocol(), is(HttpInfluxdbProtocol.class));
    }

    @Test
    public void getHostTest() {
        assert(metricsService.getHost() == getLocalName());
    }

    @Test
    public void getVersionTest() {
        assert(metricsService.getVersion().equals(hubVersion.getVersion()));
    }

    @AfterClass public static void after() {
            metricsService.stop();
    }

}

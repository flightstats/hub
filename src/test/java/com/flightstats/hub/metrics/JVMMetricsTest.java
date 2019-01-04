package com.flightstats.hub.metrics;

import metrics_influxdb.HttpInfluxdbProtocol;
import metrics_influxdb.InfluxdbProtocol;
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
    @Test
    public void registeredMetricNamesTest() {
        MetricRegistry registry = new MetricRegistry();
        HubVersion hubVersion = new HubVersion();
        JVMMetricsService metricsService = new JVMMetricsService(registry, hubVersion);
        metricsService.start();
        SortedSet<String> metricNames = metricsService.getRegisteredMetricNames();
        Iterator iterator = metricNames.iterator();
        while (iterator.hasNext()) {
            String nextString = (String) iterator.next();
            assertThat(nextString, anyOf(containsString("gc"), containsString("memory"), containsString("thread")));
            assertThat(nextString, containsString("jvm_"));
        }
        metricsService.stop();
    }

    @Test
    public void getProtocolTest() {
        MetricRegistry registry = new MetricRegistry();
        HubVersion hubVersion = new HubVersion();
        JVMMetricsService metricsService = new JVMMetricsService(registry, hubVersion);
        assertThat(metricsService.getProtocol(), is(InfluxdbProtocol.class));
        assertThat(metricsService.getProtocol(), is(HttpInfluxdbProtocol.class));
    }

    @Test
    public void getHostTest() {
        MetricRegistry registry = new MetricRegistry();
        HubVersion hubVersion = new HubVersion();
        JVMMetricsService metricsService = new JVMMetricsService(registry, hubVersion);
        assert(metricsService.getHost() == getLocalName());
    }

    @Test
    public void getVersionTest() {
        MetricRegistry registry = new MetricRegistry();
        HubVersion hubVersion = new HubVersion();
        JVMMetricsService metricsService = new JVMMetricsService(registry, hubVersion);
        assert(metricsService.getVersion().equals(hubVersion.getVersion()));
    }


}

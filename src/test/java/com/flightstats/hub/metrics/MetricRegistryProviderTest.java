package com.flightstats.hub.metrics;

import com.codahale.metrics.MetricRegistry;
import com.flightstats.hub.app.HubHost;
import com.flightstats.hub.app.HubVersion;
import org.junit.Test;
import java.util.SortedSet;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.junit.Assert.assertTrue;

public class MetricRegistryProviderTest {
    @Test
    public void testMetricRegistryProvider_metricNames() {
        HubVersion hubVersion = mock(HubVersion.class);
        when(hubVersion.getVersion()).thenReturn("local");
        MetricsConfig metricsConfig = MetricsConfig.builder()
                .appVersion(hubVersion.getVersion())
                .clusterTag("location-test")
                .env("test")
                .enabled(true)
                .hostTag(HubHost.getLocalName())
                .influxdbDatabaseName("hub_test")
                .influxdbHost("localhost")
                .influxdbPass("")
                .influxdbPort(8086)
                .influxdbProtocol("http")
                .influxdbUser("")
                .reportingIntervalSeconds(1)
                .role("hub")
                .team("testers")
                .build();
        MetricRegistry metricRegistry = new MetricRegistryProvider(metricsConfig).get();
        SortedSet<String> metrics = metricRegistry.getNames();
        boolean actual = metrics.stream()
                .allMatch(str -> str.contains("gc") || str.contains("thread") || str.contains("memory"));
        assertTrue(actual);
    }

}

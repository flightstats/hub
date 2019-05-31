package com.flightstats.hub.metrics;

import com.codahale.metrics.MetricRegistry;
import com.flightstats.hub.config.properties.MetricsProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.SortedSet;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MetricRegistryProviderTest {
    @Mock
    private MetricsProperties metricsProperties;

    @Test
    void testMetricRegistryProvider_metricNames() {
        when(metricsProperties.getReportingIntervalInSeconds()).thenReturn(1);

        MetricRegistry metricRegistry = new MetricRegistryProvider(metricsProperties).get();
        SortedSet<String> metrics = metricRegistry.getNames();
        boolean actual = metrics.stream()
                .allMatch(str -> str.contains("gc") || str.contains("thread") || str.contains("memory"));
        assertTrue(actual);
    }

}

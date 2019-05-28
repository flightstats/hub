package com.flightstats.hub.metrics;

import com.codahale.metrics.MetricRegistry;
import com.flightstats.hub.app.HubVersion;
import com.flightstats.hub.config.properties.MetricsProperties;
import com.flightstats.hub.config.properties.TickMetricsProperties;
import metrics_influxdb.measurements.MeasurementReporter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InfluxdbReporterProviderTest {

    @Mock
    private TickMetricsProperties tickMetricsProperties;
    @Mock
    private MetricsProperties metricsProperties;
    @Mock
    private MetricRegistry metricRegistry;
    @Mock
    private HubVersion hubVersion;
    private InfluxdbReporterProvider influxdbReporterProvider;

    @BeforeEach
    void setup() {
        influxdbReporterProvider =
                new InfluxdbReporterProvider(tickMetricsProperties, metricsProperties, metricRegistry, hubVersion);
    }

    @Test
    void testInfluxdbReporterGet_throwsOnBadConfig() {
        assertThrows(NullPointerException.class, influxdbReporterProvider::get);
    }

    @Test
    void testInfluxdbReporterGet_scheduledReporter() {
        when(hubVersion.getVersion()).thenReturn("local");
        when(metricsProperties.getEnv()).thenReturn("test");
        when(metricsProperties.getClusterTag()).thenReturn("location-test");
        when(metricsProperties.getRoleTag()).thenReturn("hub");
        when(metricsProperties.getTeamTag()).thenReturn("testers");

        when(tickMetricsProperties.getInfluxDbHost()).thenReturn("localhost");
        when(tickMetricsProperties.getInfluxDbUser()).thenReturn("");
        when(tickMetricsProperties.getInfluxDbPassword()).thenReturn("");
        when(tickMetricsProperties.getInfluxDbPort()).thenReturn(8086);
        when(tickMetricsProperties.getInfluxDbProtocol()).thenReturn("http");
        when(tickMetricsProperties.getInfluxDbName()).thenReturn("hub_test");

        assertEquals(influxdbReporterProvider.get().getClass(), MeasurementReporter.class);
    }
}

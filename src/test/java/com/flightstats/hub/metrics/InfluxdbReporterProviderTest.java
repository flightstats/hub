package com.flightstats.hub.metrics;

import com.codahale.metrics.MetricRegistry;
import com.flightstats.hub.app.HubHost;
import com.flightstats.hub.app.HubVersion;
import metrics_influxdb.measurements.MeasurementReporter;
import org.junit.Test;
;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class InfluxdbReporterProviderTest {

    @Test
    public void testInfluxdbReporterGet_throwsOnBadConfig() {
        MetricRegistry metricRegistry = mock(MetricRegistry.class);
        MetricsConfig metricsConfig = MetricsConfig.builder().build();

        InfluxdbReporterProvider influxdbReporterProvider = new InfluxdbReporterProvider(metricsConfig, metricRegistry);
        assertThrows(NullPointerException.class, influxdbReporterProvider::get);
    }

    @Test
    public void testInfluxdbReporterGet_scheduledReporter() {
        // GIVEN
        MetricRegistry metricRegistry = mock(MetricRegistry.class);
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
        // WHEN
         InfluxdbReporterProvider influxdbReporterProvider = new InfluxdbReporterProvider(metricsConfig, metricRegistry);

         // THEN
        assertEquals(influxdbReporterProvider.get().getClass(), MeasurementReporter.class);
    }
}

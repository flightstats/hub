package com.flightstats.hub.metrics;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;
import com.flightstats.hub.app.HubVersion;
import com.flightstats.hub.config.properties.LocalHostProperties;
import com.flightstats.hub.config.properties.MetricsProperties;
import com.flightstats.hub.config.properties.TickMetricsProperties;
import com.flightstats.hub.util.IntegrationServer;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InfluxdbReporterProviderIntegrationTest {
    private HttpServer httpServer;
    private ScheduledReporter influxdbReporter;
    private static List<String> writeResult;

    @Mock
    private TickMetricsProperties tickMetricsProperties;
    @Mock
    private MetricsProperties metricsProperties;
    @Mock
    private LocalHostProperties localHostProperties;
    @Mock
    private HubVersion hubVersion;

    private static class TestHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange httpExchange) throws IOException {
            InputStreamReader streamReader = new InputStreamReader(httpExchange.getRequestBody(), StandardCharsets.UTF_8);
            BufferedReader reader = new BufferedReader(streamReader);
            writeResult = reader
                    .lines()
                    .collect(Collectors.toList());
            reader.close();
            streamReader.close();

            httpExchange.getResponseHeaders().add("encoding", "UTF-8");
            ByteArrayOutputStream response = new ByteArrayOutputStream(writeResult.size());
            httpExchange.getResponseHeaders().set("Content-Type", "application/json");
            httpExchange.getResponseHeaders().set("Content-Length", String.valueOf(response.size()));
            httpExchange.sendResponseHeaders(200, response.size());
            response.writeTo(httpExchange.getResponseBody());
            httpExchange.close();
        }
    }

    @BeforeEach
    void setupServer() throws IOException {
        httpServer = IntegrationServer
                .builder()
                .testHandler(new TestHandler())
                .path("/write")
                .bindAddress("localhost")
                .bindPort(8086)
                .build()
                .httpServer();
        httpServer.start();
    }

    @Test
    void testInfluxdbReporterGet_reportsConfiguredTags() throws InterruptedException {
        when(hubVersion.getVersion()).thenReturn("local");
        when(metricsProperties.getEnv()).thenReturn("test");
        when(metricsProperties.getClusterTag()).thenReturn("location-test");
        when(metricsProperties.getReportingIntervalInSeconds()).thenReturn(1);
        when(metricsProperties.getRoleTag()).thenReturn("hub");
        when(metricsProperties.getTeamTag()).thenReturn("testers");


        when(tickMetricsProperties.getInfluxDbHost()).thenReturn("localhost");
        when(tickMetricsProperties.getInfluxDbUser()).thenReturn("");
        when(tickMetricsProperties.getInfluxDbPassword()).thenReturn("");
        when(tickMetricsProperties.getInfluxDbPort()).thenReturn(8086);
        when(tickMetricsProperties.getInfluxDbProtocol()).thenReturn("http");
        when(tickMetricsProperties.getInfluxDbName()).thenReturn("hub_test");

        when(localHostProperties.getName()).thenReturn("localhost");

        MetricRegistry metricRegistry = new MetricRegistryProvider(metricsProperties).get();

        InfluxdbReporterProvider influxdbReporterProvider =
                new InfluxdbReporterProvider(tickMetricsProperties, metricsProperties, metricRegistry, localHostProperties, hubVersion);

        influxdbReporter = influxdbReporterProvider.get();

        influxdbReporter.start(1, SECONDS);
        TimeUnit.MILLISECONDS.sleep(2000);
        writeResult.forEach(str -> {
            assertTrue(str.contains("cluster=location-test,env=test,"));
            assertTrue(str.contains("localhost"));
            assertTrue(str.contains(",role=hub,team=testers,version=local"));
        });
    }

    @AfterEach
    void shutdownServer() {
        influxdbReporter.stop();
        httpServer.stop(0);
    }
}

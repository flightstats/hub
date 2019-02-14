package com.flightstats.hub.metrics;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;
import com.flightstats.hub.app.HubHost;
import com.flightstats.hub.app.HubVersion;
import com.flightstats.hub.util.IntegrationServer;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class InfluxdbReporterProviderIntegrationTest {
    private HttpServer httpServer;
    private ScheduledReporter influxdbReporter;
    private static List<String> writeResult;

    private static class TestHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange httpExchange) throws IOException {

            InputStreamReader streamReader =  new InputStreamReader(httpExchange.getRequestBody(), StandardCharsets.UTF_8);
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

    @Before
    public void setupServer() throws IOException {
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
    public void testInfluxdbReporterGet_reportsConfiguredTags() throws InterruptedException {
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

        InfluxdbReporterProvider influxdbReporterProvider = new InfluxdbReporterProvider(metricsConfig, metricRegistry);
        influxdbReporter = influxdbReporterProvider.get();

        influxdbReporter.start(1, SECONDS);
        TimeUnit.MILLISECONDS.sleep(2000);
        writeResult.forEach(str -> {
            assertTrue(str.contains("cluster=location-test,env=test,"));
            assertTrue(str.contains(HubHost.getLocalName()));
            assertTrue(str.contains(",role=hub,team=testers,version=local"));
        });
    }

    @After
    public void shutdownServer() {
        influxdbReporter.stop();
        httpServer.stop(0);
    }
}

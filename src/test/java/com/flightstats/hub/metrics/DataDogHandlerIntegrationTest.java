package com.flightstats.hub.metrics;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flightstats.hub.util.IntegrationServer;
import com.flightstats.hub.util.TimeUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.StringContains.containsString;
import static org.hamcrest.number.IsCloseTo.closeTo;

class DataDogHandlerIntegrationTest {
    private static List<String> writeResult;
    private static String queryResult;
    private static HttpServer httpServer;

    private static class TestHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange httpExchange) throws IOException {

            InputStreamReader streamReader =  new InputStreamReader(httpExchange.getRequestBody(), StandardCharsets.UTF_8);
            BufferedReader reader = new BufferedReader(streamReader);
            String requestType =  httpExchange.getRequestMethod();
            if (!requestType.equalsIgnoreCase("POST")) {
                System.out.println("ERROR: method not implemented");
                ByteArrayOutputStream response = new ByteArrayOutputStream(writeResult.size());
                httpExchange.sendResponseHeaders(501, response.size());
                response.writeTo(httpExchange.getResponseBody());
                httpExchange.close();
            }
            queryResult = httpExchange.getRequestURI().getQuery();
            System.out.println("received request");
            writeResult = reader
                    .lines()
                    .collect(Collectors.toList());
            reader.close();
            streamReader.close();
            System.out.println("received writeResult" + writeResult);
            httpExchange.getResponseHeaders().add("encoding", "UTF-8");
            ByteArrayOutputStream response = new ByteArrayOutputStream(writeResult.size());
            httpExchange.getResponseHeaders().set("Content-Type", "application/json");
            httpExchange.getResponseHeaders().set("Content-Length", String.valueOf(response.size()));
            httpExchange.sendResponseHeaders(200, response.size());
            response.writeTo(httpExchange.getResponseBody());
            httpExchange.close();
        }
    }

    @BeforeAll
    static void startMockDataDogServer() throws IOException {
        httpServer = IntegrationServer.builder()
                .testHandler(new TestHandler())
                .bindAddress("localhost")
                .bindPort(8888)
                .path("/api")
                .build()
                .httpServer();
        httpServer.start();
    }

    @Test
    void testDatadogMute_mute() throws IOException {
        MetricsConfig metricsConfig = MetricsConfig.builder()
                .dataDogAPIKey("apiKey")
                .dataDogAppKey("appKey")
                .datadogApiUrl("http://localhost:8888/api")
                .hostTag("test_host")
                .build();
        DataDogHandler dataDogHandler = new DataDogHandler(metricsConfig);
        dataDogHandler.mute();

        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonExpected = mapper.createObjectNode()
                .put("message", "restarting")
                .put("scope", "name:test_host")
                .put("end", TimeUtil.now().getMillis() / 1000 + 240);
        JsonNode jsonResult = mapper.readTree(writeResult.get(0));

        assertEquals(jsonExpected.get("message"), jsonResult.get("message"));
        assertEquals(jsonExpected.get("scope"), jsonResult.get("scope"));

        assertThat(jsonResult.get("end").asDouble(), is(closeTo(jsonExpected.get("end").asDouble(), 2.0)));

        assertThat(queryResult, containsString("api_key=apiKey"));
        assertThat(queryResult, containsString("application_key=appKey"));
    }

    @AfterAll
    static void shutDownServer() {
        httpServer.stop(0);
    }

}

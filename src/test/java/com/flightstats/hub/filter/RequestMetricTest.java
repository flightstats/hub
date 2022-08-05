package com.flightstats.hub.filter;

import com.flightstats.hub.util.RequestMetric;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.HashMap;
import java.util.Optional;
import java.util.stream.Stream;

import static java.util.Collections.emptySet;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class RequestMetricTest {
    private static Stream<Arguments> provideCasesForTags() {
        String method = "GET";
        String endpoint = "/_channel_/_Y_/_M_";
        return Stream.of(
                Arguments.of(RequestMetric.Request.builder()
                                .method(method)
                                .endpoint(endpoint)
                                .channel("someChannel").build(),
                        "someChannel"),
                Arguments.of(RequestMetric.Request.builder()
                                .method(method)
                                .endpoint(endpoint)
                                .tag("someTag").build(),
                        "tag/someTag"),
                Arguments.of(RequestMetric.Request.builder()
                                .method(method)
                                .endpoint(endpoint)
                                .tag("someTag")
                                .channel("someChannel").build(),
                        "someChannel")
        );
    }
    @ParameterizedTest
    @MethodSource("provideCasesForTags")
    void testGetTagsWithChannel(RequestMetric.Request request, String expectedChannelTag) {
        HashMap<String, String> expectedTags = new HashMap<>();
        expectedTags.put("method", "GET");
        expectedTags.put("call", "GET/_channel_/_Y_/_M_");
        expectedTags.put("channel", expectedChannelTag);

        RequestMetric metric = new RequestMetric(request);
        assertEquals(expectedTags, metric.getTags());
    }

    @Test
    void testGetTagsNoChannel() {
        RequestMetric.Request request = RequestMetric.Request.builder()
                .method("GET")
                .endpoint("/internal/webhook")
                .build();

        HashMap<String, String> expectedTags = new HashMap<>();
        expectedTags.put("method", "GET");
        expectedTags.put("call", "GET/internal/webhook");

        RequestMetric metric = new RequestMetric(request);
        assertEquals(expectedTags, metric.getTags());
    }

    @Test
    void testGetTagsInternalChannel() {
        RequestMetric.Request request = RequestMetric.Request.builder()
                .method("GET")
                .endpoint("/internal/_channel_")
                .channel("some_channel")
                .build();

        HashMap<String, String> expectedTags = new HashMap<>();
        expectedTags.put("method", "GET");
        expectedTags.put("call", "GET/internal/_channel_");

        RequestMetric metric = new RequestMetric(request);
        assertEquals(expectedTags, metric.getTags());
    }

    @Test
    void testGetTagsNoEndpoint() {
        RequestMetric.Request request = RequestMetric.Request.builder()
                .method("GET")
                .path("/this/is/the/way")
                .build();

        HashMap<String, String> expectedTags = new HashMap<>();
        expectedTags.put("method", "GET");
        expectedTags.put("call", "GET/this/is/the/way");

        RequestMetric metric = new RequestMetric(request);
        assertEquals(expectedTags, metric.getTags());
    }

    private static Stream<Arguments> provideCasesForMetricName() {
        return Stream.of(
                Arguments.of(RequestMetric.Request.builder()
                                .endpoint("/shutdown").build(),
                        Optional.empty()),
                Arguments.of(RequestMetric.Request.builder()
                                .path("no endpoint?")
                                .tag("someTag").build(),
                        Optional.empty()),
                Arguments.of(RequestMetric.Request.builder()
                                .endpoint("/internal/_channel_/latest")
                                .channel("channel1").build(),
                        Optional.of("request.internal.channel")),
                Arguments.of(RequestMetric.Request.builder()
                                .endpoint("/internal/_tag_/latest")
                                .tag("tag1").build(),
                        Optional.of("request.internal.channel")),
                Arguments.of(RequestMetric.Request.builder()
                                .endpoint("/internal/health").build(),
                        Optional.of("request.internal.nonchannel")),
                Arguments.of(RequestMetric.Request.builder()
                                .endpoint("/_channel_/latest")
                                .channel("channel1").build(),
                        Optional.of("request.api.channel")),
                Arguments.of(RequestMetric.Request.builder()
                                .endpoint("/_tag_/latest")
                                .tag("tag1").build(),
                        Optional.of("request.api.channel")),
                Arguments.of(RequestMetric.Request.builder()
                                .endpoint("/health").build(),
                        Optional.of("request.api.nonchannel"))
        );
    }
    @ParameterizedTest
    @MethodSource("provideCasesForMetricName")
    void testGetMetricName(RequestMetric.Request request, Optional<String> metricName) {
        RequestMetric metric = new RequestMetric(request);
        assertEquals(metricName, metric.getMetricName());
    }

    private static Stream<Arguments> provideCasesForShouldReport() {
        return Stream.of(
                Arguments.of(RequestMetric.Request.builder()
                                .endpoint("/shutdown").build(),
                        false),
                Arguments.of(RequestMetric.Request.builder()
                                .path("no endpoint?")
                                .tag("someTag").build(),
                        false),
                Arguments.of(RequestMetric.Request.builder()
                                .endpoint("/internal/_channel_/latest")
                                .channel("IGNORE_ME_I_TEST_THINGS").build(),
                        false),
                Arguments.of(RequestMetric.Request.builder()
                                .endpoint("/internal/_channel_/latest")
                                .channel("channel1").build(),
                        true),
                Arguments.of(RequestMetric.Request.builder()
                                .endpoint("/_tag_/latest")
                                .tag("tag1").build(),
                        true)
        );
    }
    @ParameterizedTest
    @MethodSource("provideCasesForShouldReport")
    void testShouldReport(RequestMetric.Request request, boolean expectedShouldReport) {
        RequestMetric metric = new RequestMetric(request);
        assertEquals(expectedShouldReport, metric.shouldReport(emptySet(), channel -> channel.startsWith("IGNORE_ME_")));
    }
}

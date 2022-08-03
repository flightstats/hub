package com.flightstats.hub.filter;

import com.flightstats.hub.util.HubRequest;
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
                Arguments.of(HubRequest.builder()
                                .method(method)
                                .endpoint(endpoint)
                                .channel("someChannel").build(),
                        "someChannel"),
                Arguments.of(HubRequest.builder()
                                .method(method)
                                .endpoint(endpoint)
                                .tag("someTag").build(),
                        "tag/someTag"),
                Arguments.of(HubRequest.builder()
                                .method(method)
                                .endpoint(endpoint)
                                .tag("someTag")
                                .channel("someChannel").build(),
                        "someChannel")
        );
    }

    @ParameterizedTest
    @MethodSource("provideCasesForTags")
    void testGetTagsWithChannel(HubRequest request, String expectedChannelTag) {
        HashMap<String, String> expectedTags = new HashMap<>();
        expectedTags.put("method", "GET");
        expectedTags.put("call", "GET/_channel_/_Y_/_M_");
        expectedTags.put("channel", expectedChannelTag);

        RequestMetric metric = new RequestMetric(request);
        assertEquals(expectedTags, metric.getTags());
    }

    @Test
    void testGetTagsNoChannel() {
        HubRequest request = HubRequest.builder()
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
    void testGetTagsNoEndpoint() {
        HubRequest request = HubRequest.builder()
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
                Arguments.of(HubRequest.builder()
                                .endpoint("/shutdown").build(),
                        Optional.empty()),
                Arguments.of(HubRequest.builder()
                                .path("no endpoint?")
                                .tag("someTag").build(),
                        Optional.empty()),
                Arguments.of(HubRequest.builder()
                                .endpoint("/internal/_channel_/latest")
                                .channel("channel1").build(),
                        Optional.of("request.internal.channel")),
                Arguments.of(HubRequest.builder()
                                .endpoint("/internal/_tag_/latest")
                                .tag("tag1").build(),
                        Optional.of("request.internal.channel")),
                Arguments.of(HubRequest.builder()
                                .endpoint("/internal/health").build(),
                        Optional.of("request.internal.nonchannel")),
                Arguments.of(HubRequest.builder()
                                .endpoint("/_channel_/latest")
                                .channel("channel1").build(),
                        Optional.of("request.api.channel")),
                Arguments.of(HubRequest.builder()
                                .endpoint("/_tag_/latest")
                                .tag("tag1").build(),
                        Optional.of("request.api.channel")),
                Arguments.of(HubRequest.builder()
                                .endpoint("/health").build(),
                        Optional.of("request.api.nonchannel"))
        );
    }

    @ParameterizedTest
    @MethodSource("provideCasesForMetricName")
    void testGetMetricName(HubRequest request, Optional<String> metricName) {
        RequestMetric metric = new RequestMetric(request);
        assertEquals(metricName, metric.getMetricName());
    }

    private static Stream<Arguments> provideCasesForShouldReport() {
        return Stream.of(
                Arguments.of(HubRequest.builder()
                                .endpoint("/shutdown").build(),
                        false),
                Arguments.of(HubRequest.builder()
                                .path("no endpoint?")
                                .tag("someTag").build(),
                        false),
                Arguments.of(HubRequest.builder()
                                .endpoint("/internal/_channel_/latest")
                                .channel("IGNORE_ME_I_TEST_THINGS").build(),
                        false),
                Arguments.of(HubRequest.builder()
                                .endpoint("/internal/_channel_/latest")
                                .channel("channel1").build(),
                        true),
                Arguments.of(HubRequest.builder()
                                .endpoint("/_tag_/latest")
                                .tag("tag1").build(),
                        true)
        );
    }

    @ParameterizedTest
    @MethodSource("provideCasesForShouldReport")
    void testShouldReport(HubRequest request, boolean expectedShouldReport) {
        RequestMetric metric = new RequestMetric(request);
        assertEquals(expectedShouldReport, metric.shouldReport(emptySet(), channel -> channel.startsWith("IGNORE_ME_")));
    }
}

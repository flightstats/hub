package com.flightstats.hub.filter;

import com.flightstats.hub.util.HubRequest;
import com.flightstats.hub.util.RequestMetric;
import lombok.Builder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.HashMap;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.Collections.emptySet;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class RequestMetricTest {
    @Builder
    private static class Request {
        String method;
        String path;
        String endpoint;
        String channel;
        String tag;

        HubRequest asHubRequest() {
            return HubRequest.builder()
                    .method(method)
                    .path(path)
                    .endpoint(Optional.ofNullable(endpoint))
                    .channel(Optional.ofNullable(channel))
                    .tag(Optional.ofNullable(tag))
                    .build();
        }
    }

    private static HubRequest buildRequest(Function<Request.RequestBuilder, Request.RequestBuilder> buildIt) {
        Request.RequestBuilder builder = Request.builder();
        return buildIt.apply(builder).build().asHubRequest();
    }

    private static Stream<Arguments> provideCasesForTags() {
        String method = "GET";
        String endpoint = "/_channel_/_Y_/_M_";
        return Stream.of(
                Arguments.of(buildRequest(request -> request
                                .method(method)
                                .endpoint(endpoint)
                                .channel("someChannel")),
                        "someChannel"),
                Arguments.of(buildRequest(request -> request
                                .method(method)
                                .endpoint(endpoint)
                                .tag("someTag")),
                        "tag/someTag"),
                Arguments.of(buildRequest(request -> request
                                .method(method)
                                .endpoint(endpoint)
                                .tag("someTag")
                                .channel("someChannel")),
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
        HubRequest request = buildRequest(builder -> builder
                .method("GET")
                .endpoint("/internal/webhook"));

        HashMap<String, String> expectedTags = new HashMap<>();
        expectedTags.put("method", "GET");
        expectedTags.put("call", "GET/internal/webhook");

        RequestMetric metric = new RequestMetric(request);
        assertEquals(expectedTags, metric.getTags());
    }

    @Test
    void testGetTagsInternalChannel() {
        HubRequest request = buildRequest(builder -> builder
                .method("GET")
                .endpoint("/internal/_channel_")
                .channel("some_channel"));

        HashMap<String, String> expectedTags = new HashMap<>();
        expectedTags.put("method", "GET");
        expectedTags.put("call", "GET/internal/_channel_");

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
                Arguments.of(buildRequest(request -> request
                                .endpoint("/shutdown")),
                        Optional.empty()),
                Arguments.of(buildRequest(request -> request
                                .path("no endpoint?")
                                .tag("someTag")),
                        Optional.empty()),
                Arguments.of(buildRequest(request -> request
                                .endpoint("/internal/_channel_/latest")
                                .channel("channel1")),
                        Optional.of("request.internal.channel")),
                Arguments.of(buildRequest(request -> request
                                .endpoint("/internal/_tag_/latest")
                                .tag("tag1")),
                        Optional.of("request.internal.channel")),
                Arguments.of(buildRequest(request -> request
                                .endpoint("/internal/health")),
                        Optional.of("request.internal.nonchannel")),
                Arguments.of(buildRequest(request -> request
                                .endpoint("/_channel_/latest")
                                .channel("channel1")),
                        Optional.of("request.api.channel")),
                Arguments.of(buildRequest(request -> request
                                .endpoint("/_tag_/latest")
                                .tag("tag1")),
                        Optional.of("request.api.channel")),
                Arguments.of(buildRequest(request -> request
                                .endpoint("/health")),
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
                Arguments.of(buildRequest(request -> request
                                .endpoint("/shutdown")),
                        false),
                Arguments.of(buildRequest(request -> request
                                .path("no endpoint?")
                                .tag("someTag")),
                        false),
                Arguments.of(buildRequest(request -> request
                                .endpoint("/internal/_channel_/latest")
                                .channel("IGNORE_ME_I_TEST_THINGS")),
                        false),
                Arguments.of(buildRequest(request -> request
                                .endpoint("/internal/_channel_/latest")
                                .channel("channel1")),
                        true),
                Arguments.of(buildRequest(request -> request
                                .endpoint("/_tag_/latest")
                                .tag("tag1")),
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

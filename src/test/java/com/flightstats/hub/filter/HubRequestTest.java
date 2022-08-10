package com.flightstats.hub.filter;

import com.flightstats.hub.util.HubRequest;
import lombok.Builder;
import lombok.Value;
import org.glassfish.jersey.server.internal.routing.UriRoutingContext;
import org.glassfish.jersey.uri.UriTemplate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.MultivaluedHashMap;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


class HubRequestTest {
    @Value
    @Builder
    static class TestRequest {
        String uriTemplate;
        @Builder.Default
        String method = "GET";
        @Builder.Default
        Optional<String> channelAsHeader = Optional.empty();
        @Builder.Default
        Optional<String> channelAsParameter = Optional.empty();
        @Builder.Default
        Optional<String> tagAsParameter = Optional.empty();
    }

    ContainerRequestContext buildChannelRequest(TestRequest request) {
        ContainerRequestContext requestContext = mock(ContainerRequestContext.class);

        UriTemplate uriTemplate = new UriTemplate(request.getUriTemplate());
        UriRoutingContext uriInfo = mock(UriRoutingContext.class);
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getMatchedTemplates()).thenReturn(Collections.singletonList(uriTemplate));

        MultivaluedHashMap<String, String> headers = new MultivaluedHashMap<>();
        request.getChannelAsHeader().ifPresent(channel -> {
            headers.add("channelName", channel);
        });
        when(requestContext.getHeaders()).thenReturn(headers);

        MultivaluedHashMap<String, String> pathParameters = new MultivaluedHashMap<>();
        request.getChannelAsParameter().ifPresent(channel -> {
            when(requestContext.getProperty("channel")).thenReturn(channel);
            pathParameters.add("channel", channel);
        });
        request.getTagAsParameter().ifPresent(tag -> {
            when(requestContext.getProperty("tag")).thenReturn(tag);
            pathParameters.add("tag", tag);
        });
        when(uriInfo.getPathParameters()).thenReturn(pathParameters);

        when(requestContext.getMethod()).thenReturn(request.getMethod());
        return requestContext;
    }

    @Test
    void testRequestGetRequestTemplate() {
        // GIVEN
        TestRequest testRequest = TestRequest.builder()
                .uriTemplate("{channel}/{Y}/{M}/{D}/{h}/{m}/{s}/{ms}/{hash}/{direction:[n|p].*}/{count:.+}")
                .build();
        ContainerRequestContext requestContext = buildChannelRequest(testRequest);

        // WHEN
        HubRequest request = new HubRequest.RequestBuilder(requestContext).build();

        Optional<String> endpoint = request.getEndpoint();

        // THEN
        assert (endpoint.isPresent());
        assertEquals("_channel_/_Y_/_M_/_D_/_h_/_m_/_s_/_ms_/_hash_/_direction_np_/_count__", endpoint.get());
    }

    @Test
    void testRequestGetChannelAsParameter() {
        TestRequest testRequest = TestRequest.builder()
                .uriTemplate("{channel}/{Y}/{M}/{D}/{h}/{m}/{s}/{ms}/{hash}/{direction:[n|p].*}/{count:.+}")
                .channelAsParameter(Optional.of("channel1"))
                .build();
        ContainerRequestContext requestContext = buildChannelRequest(testRequest);

        HubRequest request = new HubRequest.RequestBuilder(requestContext).build();

        assertEquals(Optional.of("channel1"), request.getChannel());
    }

    @Test
    void testRequestGetChannelAsHeader() {
        TestRequest testRequest = TestRequest.builder()
                .uriTemplate("{channel}/{M}/{D}/{h}/{m}/{s}/{ms}/{hash}/{direction:[n|p].*}/{count:.+}")
                .channelAsHeader(Optional.of("channel1"))
                .build();
        ContainerRequestContext requestContext = buildChannelRequest(testRequest);

        HubRequest request = new HubRequest.RequestBuilder(requestContext).build();

        assertEquals(Optional.of("channel1"), request.getChannel());
    }

    @Test
    void testRequestGetChannelEmpty() {
        TestRequest testRequest = TestRequest.builder()
                .uriTemplate("{channel}/{Y}/{M}/{D}/{h}/{m}/{s}/{ms}/{hash}/{direction:[n|p].*}/{count:.+}")
                .build();
        ContainerRequestContext requestContext = buildChannelRequest(testRequest);

        HubRequest request = new HubRequest.RequestBuilder(requestContext).build();

        assertEquals(Optional.empty(), request.getChannel());
    }

    @Test
    void testRequestGetChannelSpecifiedAsBothParamAndHeader() {
        TestRequest testRequest = TestRequest.builder()
                .uriTemplate("{channel}/{M}/{D}/{h}/{m}/{s}/{ms}/{hash}/{direction:[n|p].*}/{count:.+}")
                .channelAsParameter(Optional.of("channelParam"))
                .channelAsHeader(Optional.of("channelHeader"))
                .build();
        ContainerRequestContext requestContext = buildChannelRequest(testRequest);

        HubRequest request = new HubRequest.RequestBuilder(requestContext).build();

        assertEquals(Optional.of("channelParam"), request.getChannel());
    }

    @Test
    void testRequestGetTagPresent() {
        TestRequest testRequest = TestRequest.builder()
                .uriTemplate("{tag}")
                .tagAsParameter(Optional.of("tag1"))
                .build();
        ContainerRequestContext requestContext = buildChannelRequest(testRequest);

        HubRequest request = new HubRequest.RequestBuilder(requestContext).build();

        assertEquals(Optional.of("tag1"), request.getTag());
    }

    @Test
    void testRequestGetTagAbsent() {
        TestRequest testRequest = TestRequest.builder()
                .uriTemplate("{tag}")
                .tagAsParameter(Optional.empty())
                .build();
        ContainerRequestContext requestContext = buildChannelRequest(testRequest);

        HubRequest request = new HubRequest.RequestBuilder(requestContext).build();

        assertEquals(Optional.empty(), request.getTag());
    }

    @Test
    void testRequestGetMethod() {
        TestRequest testRequest = TestRequest.builder()
                .uriTemplate("{tag}")
                .method("POST")
                .build();
        ContainerRequestContext requestContext = buildChannelRequest(testRequest);

        HubRequest request = new HubRequest.RequestBuilder(requestContext).build();

        assertEquals("POST", request.getMethod());
    }

    private static Stream<Arguments> provideCasesForIsShutdown() {
        return Stream.of(
                Arguments.of(null, false),
                Arguments.of("/", false),
                Arguments.of("/shutdown", true),
                Arguments.of("shutdown", false),
                Arguments.of("{channel}", false),
                Arguments.of("/shutdown?really=true", true)
        );
    }

    @ParameterizedTest
    @MethodSource("provideCasesForIsShutdown")
    void testRequestIsShutdown(String inputUrl, boolean expected) {
        Optional<String> endpoint = Optional.ofNullable(inputUrl);
        HubRequest request = HubRequest.builder()
                .endpoint(endpoint)
                .build();
        assertEquals(expected, request.isShutdown());
    }

    private static Stream<Arguments> provideCasesForIsInternal() {
        return Stream.of(
                Arguments.of(null, false),
                Arguments.of("/", false),
                Arguments.of("/internal", true),
                Arguments.of("internal", false),
                Arguments.of("/internal/channel/{channel}", true),
                Arguments.of("/infernal", false)
        );
    }

    @ParameterizedTest
    @MethodSource("provideCasesForIsInternal")
    void testRequestIsInternal(String inputUrl, boolean expected) {
        Optional<String> endpoint = Optional.ofNullable(inputUrl);
        HubRequest request = HubRequest.builder()
                .endpoint(endpoint)
                .build();
        assertEquals(expected, request.isInternal());
    }

    private static Stream<Arguments> provideCasesForIsChannelRelated() {
        HubRequest.HubRequestBuilder builder = HubRequest.builder();
        return Stream.of(
                Arguments.of(builder.channel(Optional.of("someChannel")).build(), true),
                Arguments.of(builder.tag(Optional.of("someTag")).build(), true),
                Arguments.of(builder.channel(Optional.of("someChannel")).tag(Optional.of("someTag")).build(), true)
        );
    }

    @ParameterizedTest
    @MethodSource("provideCasesForIsChannelRelated")
    void testRequestIsChannelRelated(HubRequest request, boolean isChannelRelated) {
        assertEquals(isChannelRelated, request.isChannelRelated());
    }
}

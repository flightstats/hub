package com.flightstats.hub.filter;

import com.flightstats.hub.metrics.StatsDFilter;
import com.flightstats.hub.metrics.StatsdReporter;
import com.flightstats.hub.util.RequestMetric;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MetricsRequestFilterTest {
    @Test
    void testReportTimeShouldReport() {
        StatsdReporter mockStatsdReporter = mock(StatsdReporter.class);
        StatsDFilter mockFilter = mock(StatsDFilter.class);
        RequestMetric mockMetric = mock(RequestMetric.class);
        when(mockFilter.isIgnoredGrRequestMetric(mockMetric)).thenReturn(false);

        HashMap<String, String> tags = new HashMap<>();
        tags.put("channel", "channel1");
        when(mockMetric.getTags()).thenReturn(tags);

        when(mockMetric.getMetricName()).thenReturn(Optional.of("request.api.channel"));

        MetricsRequestFilter metricsRequestFilter = new MetricsRequestFilter(mockStatsdReporter, mockFilter);
        metricsRequestFilter.reportTime(mockMetric, 1000);

        verify(mockStatsdReporter).time("request.api.channel", 1000, "channel:channel1");
    }


    @Test
    void testReportTimeShouldSkip() {
        StatsDFilter mockFilter = mock(StatsDFilter.class);
        StatsdReporter mockStatsdReporter = mock(StatsdReporter.class);

        RequestMetric mockMetric = mock(RequestMetric.class);
        when(mockFilter.isIgnoredGrRequestMetric(mockMetric)).thenReturn(true);

        MetricsRequestFilter metricsRequestFilter = new MetricsRequestFilter(mockStatsdReporter, mockFilter);
        metricsRequestFilter.reportTime(mockMetric, 1000);

        verify(mockStatsdReporter, times(0)).time(anyString(), anyLong(), any());
    }

    @Test
    void testReportErrorIfErrorCode() {
        MetricsRequestFilter.RequestState requestState = mock(MetricsRequestFilter.RequestState.class);
        when(requestState.isErrorStatusCode()).thenReturn(true);
        when(requestState.getStatusCode()).thenReturn(500);

        RequestMetric mockMetric = mock(RequestMetric.class);
        HashMap<String, String> tags = new HashMap<>();
        tags.put("channel", "channel1");
        tags.put("call", "callPath");
        when(mockMetric.getTags()).thenReturn(tags);
        when(requestState.getRequestMetric()).thenReturn(mockMetric);

        StatsdReporter mockStatsdReporter = mock(StatsdReporter.class);
        StatsDFilter mockFilter = mock(StatsDFilter.class);
        MetricsRequestFilter metricsRequestFilter = new MetricsRequestFilter(mockStatsdReporter, mockFilter);

        metricsRequestFilter.reportError(requestState);
        verify(mockStatsdReporter).count("errors", 1, "call:callPath", "channel:channel1", "errorCode:500");
    }

    @Test
    void testReportErrorSkipsIfNotError() {
        MetricsRequestFilter.RequestState requestState = mock(MetricsRequestFilter.RequestState.class);
        when(requestState.isErrorStatusCode()).thenReturn(false);

        StatsdReporter mockStatsdReporter = mock(StatsdReporter.class);
        StatsDFilter mockFilter = mock(StatsDFilter.class);
        MetricsRequestFilter metricsRequestFilter = new MetricsRequestFilter(mockStatsdReporter, mockFilter);

        metricsRequestFilter.reportError(requestState);
        verify(mockStatsdReporter, times(0)).count(anyString(), anyLong(), any());
    }

    private static Stream<Arguments> getTestCasesForIsErrorStatusCode() {
        return Stream.of(
                Arguments.of(200, false),
                Arguments.of(399, false),
                Arguments.of(400, false),
                Arguments.of(401, true),
                Arguments.of(403, true),
                Arguments.of(404, false),
                Arguments.of(500, true)
        );
    }

    @ParameterizedTest
    @MethodSource("getTestCasesForIsErrorStatusCode")
    void testRequestStateIsErrorStatusCode(int statusCode, boolean expected) {
        ContainerRequestContext mockRequest = mock(ContainerRequestContext.class);
        ContainerResponseContext mockResponse = mock(ContainerResponseContext.class);
        RequestMetric mockMetric = mock(RequestMetric.class);

        when(mockResponse.getStatus()).thenReturn(statusCode);
        MetricsRequestFilter.RequestState requestState = new MetricsRequestFilter.RequestState(mockRequest, mockMetric);
        requestState.setResponse(mockResponse);

        assertEquals(statusCode, requestState.getStatusCode());
        assertEquals(expected, requestState.isErrorStatusCode());
    }
}

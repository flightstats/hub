package com.flightstats.hub.filter;

import com.flightstats.hub.metrics.StatsDFilter;
import com.flightstats.hub.metrics.StatsdReporter;
import com.flightstats.hub.util.RequestMetric;
import com.google.common.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;
import java.util.Arrays;
import java.util.Map;

/**
 * Filter class to handle intercepting requests and responses from the Hub and sending metrics
 */
@Slf4j
@Provider
public class MetricsRequestFilter implements ContainerRequestFilter, ContainerResponseFilter {
    private static final ThreadLocal<RequestState> threadLocal = new ThreadLocal<>();

    private final StatsdReporter statsdReporter;
    private final StatsDFilter statsdFilter;

    @Inject
    public MetricsRequestFilter(StatsdReporter statsdReporter, StatsDFilter statsdFilter) {
        this.statsdReporter = statsdReporter;
        this.statsdFilter = statsdFilter;
    }

    public void finalStats() {
        try {
            RequestState requestState = threadLocal.get();
            if (null == requestState) {
                return;
            }

            reportTime(requestState.getRequestMetric(), requestState.getStart());
            reportError(requestState);
        } catch (Exception e) {
            log.error("metrics request error", e);
        } finally {
            threadLocal.remove();
        }
    }

    @VisibleForTesting
    void reportTime(RequestMetric metric, long startTime) {
        long time = System.currentTimeMillis() - startTime;
        log.trace("request {}, time: {}", metric.getTags().get("endpoint"), time);
        if (statsdFilter.isIgnoredRequestMetric(metric)) {
            return;
        }

        String[] tagArray = getTagArray(metric.getTags());
        log.trace("statsdReporter data sent: {}", Arrays.toString(tagArray));
        metric.getMetricName().ifPresent(metricName ->
                statsdReporter.time(metricName, startTime, tagArray));
    }

    @VisibleForTesting
    void reportError(RequestState requestState) {
        if (!requestState.isErrorStatusCode()) {
            return;
        }

        RequestMetric metric = requestState.getRequestMetric();
        metric.getTags().put("errorCode", String.valueOf(requestState.getStatusCode()));
        String[] tagArray = getTagArray(metric.getTags(), "errorCode", "call", "channel");
        log.trace("data sent: {}", Arrays.toString(tagArray));
        statsdReporter.count("errors", 1, tagArray);
    }

    private String[] getTagArray(Map<String, String> tags, String... tagsOnly) {
        return tags.entrySet().stream()
                .filter(entry -> {
                    if (tagsOnly != null && tagsOnly.length > 0) {
                        return Arrays.asList(tagsOnly).contains(entry.getKey());
                    } else {
                        return true;
                    }
                })
                .map(entry -> entry.getKey() + ":" + entry.getValue())
                .toArray(String[]::new);
    }

    @Override
    public void filter(ContainerRequestContext request, ContainerResponseContext response) {
        try {
            RequestState requestState = threadLocal.get();
            if (null != requestState) {
                requestState.setResponse(response);
            }
        } catch (Exception e) {
            log.error("DataDog request error", e);
        }
    }

    @Override
    public void filter(ContainerRequestContext request) {
        threadLocal.set(new RequestState(request));
    }

    @VisibleForTesting
    static class RequestState {
        private final long start = System.currentTimeMillis();
        private final ContainerRequestContext request;
        private final RequestMetric requestMetric;
        private ContainerResponseContext response;

        RequestState(ContainerRequestContext request) {
            this.request = request;
            this.requestMetric = new RequestMetric(request);
        }

        RequestState(ContainerRequestContext request, RequestMetric requestMetric) {
            this.request = request;
            this.requestMetric = requestMetric;
        }

        public long getStart() {
            return start;
        }

        public ContainerRequestContext getRequest() {
            return request;
        }

        public ContainerResponseContext getResponse() {
            return response;
        }

        public RequestMetric getRequestMetric() {
            return requestMetric;
        }

        public boolean isErrorStatusCode() {
            return getStatusCode() > 400 && getStatusCode() != 404;
        }

        public int getStatusCode() {
            return getResponse().getStatus();
        }

        public void setResponse(ContainerResponseContext response) {
            this.response = response;
        }
    }
}
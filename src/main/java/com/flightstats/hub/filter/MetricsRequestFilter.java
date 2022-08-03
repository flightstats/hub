package com.flightstats.hub.filter;

import com.flightstats.hub.metrics.StatsDFilter;
import com.flightstats.hub.metrics.StatsdReporter;
import com.flightstats.hub.util.RequestMetric;
import com.flightstats.hub.util.RequestUtils;
import com.google.common.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.jersey.server.internal.routing.UriRoutingContext;
import org.glassfish.jersey.uri.UriTemplate;

import javax.inject.Inject;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * Filter class to handle intercepting requests and responses from the Hub and sending metrics
 */
@Slf4j
@Provider
public class MetricsRequestFilter implements ContainerRequestFilter, ContainerResponseFilter {
    private static final ThreadLocal<RequestState> threadLocal = new ThreadLocal<>();
    private static final String CHARACTERS_TO_REMOVE = "[\\[\\]|.*+]";
    private static final String CHARACTERS_TO_REPLACE = "[:\\{\\}]";

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

            ContainerRequestContext request = requestState.getRequest();
            long time = System.currentTimeMillis() - requestState.getStart();
            RequestMetric metric = new RequestMetric(request);

            if (metric.shouldReport(statsdFilter.getRequestMetricsToIgnore(), statsdFilter::isTestChannel)) {
                String[] tagArray = getTagArray(metric.getTags());
                log.trace("statsdReporter data sent: {}", Arrays.toString(tagArray));
                statsdReporter.time("request." + metric, requestState.getStart(), tagArray);
            }
            log.trace("request {}, time: {}", metric.getTags().get("endpoint"), time);
            int returnCode = requestState.getResponse().getStatus();
            if (returnCode > 400 && returnCode != 404) {
                metric.getTags().put("errorCode", String.valueOf(returnCode));
                String[] tagArray = getTagArray(metric.getTags(), "errorCode", "call", "channel");
                log.trace("data sent: {}", Arrays.toString(tagArray));
                statsdReporter.count("errors", 1, tagArray);
            }
        } catch (Exception e) {
            log.error("metrics request error", e);
        } finally {
            threadLocal.remove();
        }
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

    private class RequestState {
        private final long start = System.currentTimeMillis();
        private final ContainerRequestContext request;
        private ContainerResponseContext response;

        RequestState(ContainerRequestContext request) {
            this.request = request;
        }

        public long getStart() {
            return this.start;
        }

        public ContainerRequestContext getRequest() {
            return this.request;
        }

        public ContainerResponseContext getResponse() {
            return this.response;
        }

        public void setResponse(ContainerResponseContext response) {
            this.response = response;
        }
    }
}
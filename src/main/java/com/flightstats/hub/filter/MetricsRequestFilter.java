package com.flightstats.hub.filter;

import com.flightstats.hub.app.HubProvider;
import com.flightstats.hub.metrics.StatsDFilter;
import com.flightstats.hub.metrics.StatsdReporter;
import com.flightstats.hub.util.RequestUtils;
import com.google.common.annotations.VisibleForTesting;
import org.glassfish.jersey.server.internal.routing.UriRoutingContext;
import org.glassfish.jersey.uri.UriTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * Filter class to handle intercepting requests and responses from the Hub and sending metrics
 */
@Provider
public class MetricsRequestFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger logger = LoggerFactory.getLogger(MetricsRequestFilter.class);
    private static final StatsdReporter statsdReporter = HubProvider.getInstance(StatsdReporter.class);
    private static final StatsDFilter statsdFilter = HubProvider.getInstance(StatsDFilter.class);
    private static final ThreadLocal<RequestState> threadLocal = new ThreadLocal<>();
    private static final String CHARACTERS_TO_REMOVE = "[\\[\\]|.*+]";
    private static final String CHARACTERS_TO_REPLACE = "[:\\{\\}]";

    public static void finalStats() {
        try {
            RequestState requestState = threadLocal.get();
            if (null == requestState) {
                return;
            }

            ContainerRequestContext request = requestState.getRequest();
            long time = System.currentTimeMillis() - requestState.getStart();
            String endpoint = getRequestTemplate(request);

            Map<String, String> tags = new HashMap<>();
            tags.put("method", request.getMethod());
            tags.put("call", tags.get("method") + endpoint);

            String channel = RequestUtils.getChannelName(request);
            if (!isBlank(channel)) {
                tags.put("channel", channel);
            }
            String tag = RequestUtils.getTag(request);
            if (!isBlank(tag)) {
                tags.put("tag", tag);
            }

            if (isBlank(endpoint)) {
                logger.trace("no endpoint, path: {}", request.getUriInfo().getPath());
            } else if (tags.get("call").endsWith("/shutdown")) {
                logger.info("call to shutdown, ignoring statsd time {}", time);
            } else {
                String[] tagArray = getTagArray(tags);
                if (!statsdFilter.isTestChannel(channel)) {
                    logger.trace("statsdReporter data sent: {}", Arrays.toString(tagArray));
                    statsdReporter.requestTime(requestState.getStart(), tagArray);
                }
            }
            logger.trace("request {}, time: {}", tags.get("endpoint"), time);
            int returnCode = requestState.getResponse().getStatus();
            if (returnCode > 400 && returnCode != 404) {
                tags.put("errorCode", String.valueOf(returnCode));
                String[] tagArray = getTagArray(tags, "errorCode", "call", "channel");
                logger.trace("data sent: {}", Arrays.toString(tagArray));
                statsdReporter.count("errors", 1, tagArray);
            }
        } catch (Exception e) {
            logger.error("metrics request error", e);
        } finally {
            threadLocal.remove();
        }
    }

    private static String[] getTagArray(Map<String, String> tags, String... tagsOnly) {
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

    @VisibleForTesting
    static String getRequestTemplate(ContainerRequestContext request) {
        UriRoutingContext uriInfo = (UriRoutingContext) request.getUriInfo();
        ArrayList<UriTemplate> templateList = new ArrayList<>(uriInfo.getMatchedTemplates());
        Collections.reverse(templateList);
        return templateList
                .stream()
                .map(UriTemplate::getTemplate)
                .map(template -> template.replaceAll(CHARACTERS_TO_REMOVE, ""))
                .map(template -> template.replaceAll(CHARACTERS_TO_REPLACE, "_"))
                .collect(Collectors.joining(""));
    }

    @Override
    public void filter(ContainerRequestContext request, ContainerResponseContext response) {
        try {
            RequestState requestState = threadLocal.get();
            if (null != requestState) {
                requestState.setResponse(response);
            }
        } catch (Exception e) {
            logger.error("DataDog request error", e);
        }
    }

    @Override
    public void filter(ContainerRequestContext request) throws IOException {
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

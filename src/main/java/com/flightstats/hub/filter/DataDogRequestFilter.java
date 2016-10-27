package com.flightstats.hub.filter;

import com.flightstats.hub.metrics.DataDog;
import com.flightstats.hub.util.RequestUtils;
import com.google.common.annotations.VisibleForTesting;
import com.timgroup.statsd.StatsDClient;
import lombok.Getter;
import lombok.Setter;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * Filter class to handle intercepting requests and responses from the Hub and pipe statistics to
 * DogStatsD (DataDog) agent running on the server.
 */
@Provider
public class DataDogRequestFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger logger = LoggerFactory.getLogger(DataDogRequestFilter.class);
    private final static StatsDClient statsd = DataDog.statsd;
    private static final ThreadLocal<DataDogState> threadLocal = new ThreadLocal<>();
    private static final String CHARACTERS_TO_REMOVE = "[\\[\\]|.*+]";
    private static final String CHARACTERS_TO_REPLACE = "[:\\{\\}]";

    @Override
    public void filter(ContainerRequestContext request, ContainerResponseContext response) {
        try {
            DataDogState dataDogState = threadLocal.get();
            if (null != dataDogState) {
                dataDogState.setResponse(response);
            }
        } catch (Exception e) {
            logger.error("DataDog request error", e);
        }
    }

    public static void finalStats() {
        try {
            DataDogState dataDogState = threadLocal.get();
            if (null == dataDogState) return;

            ContainerRequestContext request = dataDogState.getRequest();
            long time = System.currentTimeMillis() - dataDogState.getStart();

            Map<String, String> tags = new HashMap<>();
            tags.put("method", request.getMethod());
            tags.put("endpoint", getRequestTemplate(request));
            tags.put("call", tags.get("method") + tags.get("endpoint"));

            String channel = RequestUtils.getChannelName(request);
            if (!isBlank(channel)) {
                tags.put("channel", channel);
            }

            String tag = RequestUtils.getTag(request);
            if (!isBlank(tag)) {
                tags.put("tag", tag);
            }

            String[] tagArray = tags.entrySet().stream()
                    .map(entry -> entry.getKey() + ":" + entry.getValue())
                    .toArray(String[]::new);

            if (tags.get("endpoint").isEmpty()) {
                logger.trace("DataDog no endpoint, path: {}", request.getUriInfo().getPath());
            } else if (tags.get("endpoint").equals("/shutdown")) {
                logger.info("call to shutdown, ignoring datadog time {}", time);
            } else {
                logger.info("DataDog data sent: {}", Arrays.toString(tagArray));
                statsd.recordExecutionTime("request", time, tagArray);
                statsd.incrementCounter("request", tagArray);
            }

            logger.trace("DataDog request {}, time: {}", tags.get("endpoint"), time);
            int returnCode = dataDogState.getResponse().getStatus();
            if (returnCode > 400 && returnCode != 404) {
                statsd.incrementCounter("errors", "errorCode:" + returnCode, tags.get("call"));
            }
        } catch (Exception e) {
            logger.error("DataDog request error", e);
        } finally {
            threadLocal.remove();
        }
    }

    @Override
    public void filter(ContainerRequestContext request) throws IOException {
        threadLocal.set(new DataDogState(request));
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

    @Setter
    @Getter
    private class DataDogState {
        private final long start = System.currentTimeMillis();
        private final ContainerRequestContext request;
        private ContainerResponseContext response;

        DataDogState(ContainerRequestContext request) {
            this.request = request;
        }

    }
}

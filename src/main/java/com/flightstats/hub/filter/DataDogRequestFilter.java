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
import java.util.*;
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
            if (null == dataDogState) {
                return;
            }

            ContainerRequestContext request = dataDogState.getRequest();
            long time = System.currentTimeMillis() - dataDogState.getStart();
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
                logger.trace("DataDog no endpoint, path: {}", request.getUriInfo().getPath());
            } else if (tags.get("call").endsWith("/shutdown")) {
                logger.info("call to shutdown, ignoring datadog time {}", time);
            } else {
                String[] tagArray = getTagArray(tags);
                logger.info("DataDog data sent: {}", Arrays.toString(tagArray));
                statsd.recordExecutionTime("request", time, tagArray);
                statsd.incrementCounter("request", tagArray);
            }

            logger.trace("DataDog request {}, time: {}", tags.get("endpoint"), time);
            int returnCode = dataDogState.getResponse().getStatus();
            if (returnCode > 400 && returnCode != 404) {
                tags.put("errorCode", String.valueOf(returnCode));
                String[] tagArray = getTagArray(tags, "errorCode", "call", "channel");
                logger.trace("DataDog data sent: {}", Arrays.toString(tagArray));
                statsd.incrementCounter("errors", tagArray);
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

package com.flightstats.hub.filter;

import com.flightstats.hub.metrics.DataDog;
import com.flightstats.hub.util.ChannelNameUtils;
import com.timgroup.statsd.StatsDClient;
import org.apache.commons.lang3.StringUtils;
import org.glassfish.jersey.server.internal.routing.UriRoutingContext;
import org.glassfish.jersey.uri.UriTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Filter class to handle intercepting requests and responses from the Hub and pipe statistics to
 * DogStatsD (DataDog) agent running on the server.
 */
@Provider
public class DataDogRequestFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger logger = LoggerFactory.getLogger(DataDogRequestFilter.class);
    private final static StatsDClient statsd = DataDog.statsd;
    private static final ThreadLocal<Long> threadStartTime = new ThreadLocal<>();
    private static final String INVALID_CHARACTERS = "[\\[\\]]";

    @Override
    public void filter(ContainerRequestContext request, ContainerResponseContext response) throws IOException {
        try {
            String channelName = channelName(request);
            String method = request.getMethod();
            String endpoint = getRequestTemplate(request);
            long time = System.currentTimeMillis() - threadStartTime.get();
            if (endpoint.isEmpty()) {
                logger.trace("DataDog no endpoint, path: {}", request.getUriInfo().getPath());
            } else if (endpoint.equals("/shutdown")) {
                logger.info("call to shutdown, ignoring datadog time {}", time);
            } else {
                statsd.recordExecutionTime("request", time, "channel:" + channelName, "method:" + method, "endpoint:" + endpoint);
                statsd.incrementCounter("request", "channel:" + channelName, "method:" + method, "endpoint:" + endpoint);
            }
            logger.trace("DataDog request {}, time: {}", endpoint, time);
        } catch (Exception e) {
            logger.error("DataDog request error", e);
        }
        int returnCode = response.getStatus();
        if (returnCode > 400 && returnCode != 404) {
            statsd.incrementCounter("errors", "errorCode:" + returnCode);
        }
    }

    @Override
    public void filter(ContainerRequestContext request) throws IOException {
        threadStartTime.set(System.currentTimeMillis());
    }

    private String channelName(ContainerRequestContext request) {
        String name;
        try {
            name = ChannelNameUtils.parseChannelName(request.getUriInfo().getRequestUri().getPath());

            if (StringUtils.isBlank(name)) {
                MultivaluedMap<String, String> headers = request.getHeaders();
                List<String> results = headers != null ? headers.get("channelName") : null;
                name = results != null ? results.get(0) : "";
            }
        } catch (Exception e) {
            name = "";
        }
        return name;
    }

    private String getRequestTemplate(ContainerRequestContext request) {
        UriRoutingContext uriInfo = (UriRoutingContext) request.getUriInfo();
        ArrayList<UriTemplate> templateList = new ArrayList<>(uriInfo.getMatchedTemplates());
        Collections.reverse(templateList);
        return templateList
                .stream()
                .map(UriTemplate::getTemplate)
                .map(template -> template.replaceAll(INVALID_CHARACTERS, "_"))
                .collect(Collectors.joining());
    }
}

package com.flightstats.hub.util;

import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.jersey.server.internal.routing.UriRoutingContext;
import org.glassfish.jersey.uri.UriTemplate;

import javax.ws.rs.container.ContainerRequestContext;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Value
@Slf4j
public class RequestMetric {
    Request request;

    public RequestMetric(Request request) {
        this.request = request;
    }

    public RequestMetric(ContainerRequestContext requestContext) {
        this.request = new RequestBuilder(requestContext).build();
    }

    @Builder
    @Value
    public static class Request {
        String endpoint;
        String path;
        String method;
        String channel;
        String tag;

        public Optional<String> getEndpoint() {
            return convertToOptional(endpoint);
        }

        public Optional<String> getChannel() {
            return convertToOptional(channel);
        }

        public Optional<String> getTag() {
            return convertToOptional(tag);
        }

        public boolean isInternal() {
            return getEndpoint().map(endpoint -> endpoint.startsWith("/internal")).orElse(false);
        }

        public boolean isShutdown() {
            return getEndpoint().map(endpoint -> endpoint.startsWith("/shutdown")).orElse(false);
        }

        public boolean isChannelRelated() {
            return getChannel().isPresent() || getTag().isPresent();
        }

        private Optional<String> convertToOptional(String string) {
            return Optional.ofNullable(string).filter(value -> !value.isEmpty());
        }
    }

    public static class RequestBuilder {
        private static final String CHARACTERS_TO_REMOVE = "[\\[\\]|.*+]";
        private static final String CHARACTERS_TO_REPLACE = "[:\\{\\}]";

        ContainerRequestContext request;

        public RequestBuilder(ContainerRequestContext request) {
            this.request = request;
        }

        public Request build() {
            return Request.builder()
                    .endpoint(getEndpoint())
                    .channel(getChannel())
                    .tag(getPathParameter("tag"))
                    .method(request.getMethod())
                    .path(request.getUriInfo().getPath())
                    .build();
        }
        public String getEndpoint() {
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

        public String getChannel() {
            return Stream.of(getPathParameter("channel"), getHeader("channelName"))
                    .filter(value -> value != null && !value.isEmpty())
                    .findFirst().orElse("");
        }

        private String getPathParameter(String key) {
            return request.getUriInfo().getPathParameters().getFirst(key);
        }

        private String getHeader(String key) {
            return request.getHeaders().getFirst(key);
        }
    }

    public Map<String, String> getTags() {
        Map<String, String> tags = new HashMap<>();
        tags.put("method", request.getMethod());
        tags.put("call", getCall());

        if (!request.isInternal()) {
            getChannelTag().ifPresent(name -> tags.put("channel", name));
        }

        return tags;
    }
    public Optional<String> getMetricName() {
        if (!request.getEndpoint().isPresent()) {
            log.trace("no endpoint, path: {}", request.getPath());
            return Optional.empty();
        }
        else if (request.isShutdown()) {
            log.info("call to shutdown, ignoring statsd time");
            return Optional.empty();
        }

        String requestScope = request.isInternal() ? "internal" : "api";
        String channelDifferentiator = request.isChannelRelated() ? "channel" : "nonchannel";
        return Optional.of(String.format("request.%s.%s", requestScope, channelDifferentiator));
    }

    public boolean shouldReport(Set<String> metricsToIgnore, Function<String, Boolean> testChannelCheck) {
        boolean isMetricToIgnore = getMetricName().map(metricsToIgnore::contains).orElse(true);
        boolean isTestChannel = request.getChannel().map(testChannelCheck).orElse(false);

        return !isMetricToIgnore && !isTestChannel;
    }


    private Optional<String> getChannelTag() {
        Optional<String> tagAsChannel = request.getTag().map(tag -> "tag/" + tag);
        return Stream.of(request.getChannel(), tagAsChannel)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst();
    }

    private String getCall() {
        return request.getMethod() + request.getEndpoint().orElse(request.getPath());
    }

}

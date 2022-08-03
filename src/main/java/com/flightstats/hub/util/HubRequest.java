package com.flightstats.hub.util;

import lombok.Builder;
import lombok.Value;
import org.glassfish.jersey.server.internal.routing.UriRoutingContext;
import org.glassfish.jersey.uri.UriTemplate;

import javax.ws.rs.container.ContainerRequestContext;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Builder
@Value
public class HubRequest {
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

    public static class RequestBuilder {
        private static final String CHARACTERS_TO_REMOVE = "[\\[\\]|.*+]";
        private static final String CHARACTERS_TO_REPLACE = "[:\\{\\}]";

        private final ContainerRequestContext request;

        public RequestBuilder(ContainerRequestContext request) {
            this.request = request;
        }

        public HubRequest build() {
            return HubRequest.builder()
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
}

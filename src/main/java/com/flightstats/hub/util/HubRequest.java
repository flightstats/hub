package com.flightstats.hub.util;

import lombok.Builder;
import lombok.Value;
import org.glassfish.jersey.server.internal.routing.UriRoutingContext;
import org.glassfish.jersey.uri.UriTemplate;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.container.ContainerRequestContext;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Builder
@Value
public class HubRequest {
    @Builder.Default
    Optional<String> endpoint = Optional.empty();
    String path;
    String method;
    @Builder.Default
    Optional<String> channel = Optional.empty();
    @Builder.Default
    Optional<String> tag = Optional.empty();

    public boolean isInternal() {
        return getEndpoint().map(endpoint -> endpoint.startsWith("/internal")).orElse(false);
    }

    public boolean isShutdown() {
        return getEndpoint().map(endpoint -> endpoint.startsWith("/shutdown")).orElse(false);
    }

    public boolean isChannelRelated() {
        return getChannel().isPresent() || getTag().isPresent();
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

        public Optional<String> getEndpoint() {
            UriRoutingContext uriInfo = (UriRoutingContext) request.getUriInfo();
            ArrayList<UriTemplate> templateList = new ArrayList<>(uriInfo.getMatchedTemplates());
            Collections.reverse(templateList);
            String endpoint = templateList
                    .stream()
                    .map(UriTemplate::getTemplate)
                    .map(template -> template.replaceAll(CHARACTERS_TO_REMOVE, ""))
                    .map(template -> template.replaceAll(CHARACTERS_TO_REPLACE, "_"))
                    .collect(Collectors.joining(""));
            return convertToOptional(endpoint);
        }

        public Optional<String> getChannel() {
            return Stream.of(getPathParameter("channel"), getHeader("channelName"))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .findFirst();
        }

        private Optional<String> getPathParameter(String key) {
            return convertToOptional(request.getUriInfo().getPathParameters().getFirst(key));
        }

        private Optional<String> getHeader(String key) {
            return convertToOptional(request.getHeaders().getFirst(key));
        }

        private Optional<String> convertToOptional(String string) {
            return Optional.ofNullable(string).filter(value -> !value.isEmpty());
        }

    }
}

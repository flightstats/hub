package com.flightstats.hub.util;

import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.container.ContainerRequestContext;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

@Slf4j
public class RequestMetric {
    private final HubRequest request;

    public RequestMetric(HubRequest request) {
        this.request = request;
    }

    public RequestMetric(ContainerRequestContext requestContext) {
        this.request = new HubRequest.RequestBuilder(requestContext).build();
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
        } else if (request.isShutdown()) {
            log.info("call to shutdown, ignoring statsd time");
            return Optional.empty();
        }

        String requestScope = request.isInternal() ? "internal" : "api";
        String channelDifferentiator = request.isChannelRelated() ? "channel" : "nonchannel";
        return Optional.of(String.format("request.%s.%s", requestScope, channelDifferentiator));
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

package com.flightstats.hub.model;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Builder
@Value
public class WebhookErrors {

    private final Object _links;
    private final List<Error> errors;

    @Builder
    @Value
    public static class Error {
        private final String name;
        private final List<String> errors;
    }

}

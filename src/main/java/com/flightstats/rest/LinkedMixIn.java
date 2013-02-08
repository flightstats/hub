package com.flightstats.rest;

import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonUnwrapped;

public abstract class LinkedMixIn<T> {

    @JsonProperty("_links")
    private final HalLinks links;

    @JsonProperty @JsonUnwrapped
    private final T object;

    public LinkedMixIn(@JsonProperty("_links") HalLinks links, @JsonUnwrapped T object) {
        throw new RuntimeException("Do not instantiate");
    }
}

package com.flightstats.rest;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.flightstats.jackson.AbstractMixIn;

@AbstractMixIn
public abstract class LinkedMixIn<T> extends Linked<T> {

    @JsonProperty("_links")
    private HalLinks halLinks;

    @JsonProperty @JsonUnwrapped
    private T object;

    public LinkedMixIn(@JsonProperty("_links") HalLinks halLinks, @JsonUnwrapped T object) {
        super(halLinks, object);
    }
}

package com.flightstats.datahub.model.serialize;

import org.codehaus.jackson.annotate.JsonProperty;

public abstract class ChannelCreationRequestMixIn {

    @JsonProperty("name")
    private String name;

    @JsonProperty("description")
    private String description;

    public ChannelCreationRequestMixIn(@JsonProperty("name") String name, @JsonProperty("description") String description) {
        throw new IllegalStateException("Do not instantiate mix-in configuration classes.");
    }
}

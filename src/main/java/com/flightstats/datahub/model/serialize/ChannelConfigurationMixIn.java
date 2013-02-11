package com.flightstats.datahub.model.serialize;

import org.codehaus.jackson.annotate.JsonProperty;

import java.util.Date;

public abstract class ChannelConfigurationMixIn {

    public ChannelConfigurationMixIn(@JsonProperty("name") String name, @JsonProperty("creationDate") Date creationDate) {
        throw new IllegalStateException("Do not instantiate mix-in configuration classes.");
    }

}

package com.flightstats.datahub.model.serialize;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.flightstats.datahub.model.ChannelConfiguration;
import com.flightstats.datahub.model.MetadataResponse;
import com.flightstats.jackson.AbstractMixIn;

import java.util.Date;

@AbstractMixIn
public abstract class MetadataResponseMixIn extends MetadataResponse {

    public MetadataResponseMixIn(ChannelConfiguration config, Date lastUpdateDate) {
        super(config, lastUpdateDate);
    }

    @JsonProperty("name")
    public abstract String getName();

    @JsonProperty("creationDate")
    public abstract Date getCreationDate();

    @JsonProperty("lastUpdateDate")
    public abstract Date getLastUpdateDate();

    @JsonProperty("ttlMillis")
    public abstract Long getTtlMillis();

}

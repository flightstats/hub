package com.flightstats.datahub.model.serialize;

import org.codehaus.jackson.annotate.JsonProperty;

import java.util.Date;
import java.util.UUID;

public abstract class ValueInsertionResultMixIn {

    @JsonProperty("id")
    public UUID getId() {
        throw new UnsupportedOperationException("MixIn classes should not be used directly.");
    }

    @JsonProperty("timestamp")
    public Date getDate() {
        throw new UnsupportedOperationException("MixIn classes should not be used directly.");

    }
}

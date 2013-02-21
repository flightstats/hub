package com.flightstats.datahub.model.serialize;

import com.flightstats.datahub.model.DataHubKey;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.Date;

public abstract class ValueInsertionResultMixIn {

    @JsonProperty("id")
    public DataHubKey getKey() {
        throw new UnsupportedOperationException("MixIn classes should not be used directly.");
    }

    @JsonProperty("timestamp")
    public Date getDate() {
        throw new UnsupportedOperationException("MixIn classes should not be used directly.");

    }
}

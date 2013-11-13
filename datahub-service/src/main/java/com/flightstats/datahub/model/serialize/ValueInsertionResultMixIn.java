package com.flightstats.datahub.model.serialize;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.flightstats.datahub.model.DataHubKey;
import com.flightstats.datahub.model.ValueInsertionResult;
import com.flightstats.jackson.AbstractMixIn;

import java.util.Date;

@AbstractMixIn
public abstract class ValueInsertionResultMixIn extends ValueInsertionResult {

    public ValueInsertionResultMixIn(DataHubKey key, String rowKey) {
        super(key, rowKey);
    }

    @JsonIgnore
    public abstract DataHubKey getKey();

	@JsonProperty("timestamp")
    public abstract Date getDate();
}

package com.flightstats.hub.model.serialize;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.ValueInsertionResult;
import com.flightstats.jackson.AbstractMixIn;

import java.util.Date;

@AbstractMixIn
public abstract class ValueInsertionResultMixIn extends ValueInsertionResult {

    public ValueInsertionResultMixIn(ContentKey key, String rowKey, Date date) {
        super(key, date);
    }

    @JsonIgnore
    @Override
    public abstract ContentKey getKey();

	@JsonProperty("timestamp")
    @Override
    public abstract Date getDate();
}

package com.flightstats.datahub.model.serialize;

import com.flightstats.datahub.model.DataHubKey;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.Date;

public abstract class ValueInsertionResultMixIn {

	@JsonIgnore
	abstract public DataHubKey getKey();

	@JsonProperty("timestamp")
	abstract public Date getDate();
}

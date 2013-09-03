package com.flightstats.datahub.model.serialize;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.Date;

public abstract class ChannelConfigurationMixIn {

	public ChannelConfigurationMixIn(@JsonProperty("name") String name, @JsonProperty("creationDate") Date creationDate, @JsonProperty("ttlMillis") Long ttlMillis) {
		throw new IllegalStateException("Do not instantiate mix-in configuration classes.");
	}

	@JsonIgnore
	abstract public Date getLastUpdateDate();

}

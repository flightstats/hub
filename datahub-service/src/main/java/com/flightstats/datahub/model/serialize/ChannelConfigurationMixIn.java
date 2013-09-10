package com.flightstats.datahub.model.serialize;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.flightstats.datahub.model.ChannelConfiguration;
import com.flightstats.jackson.AbstractMixIn;

import java.util.Date;

@AbstractMixIn
public abstract class ChannelConfigurationMixIn extends ChannelConfiguration {

	public ChannelConfigurationMixIn(@JsonProperty("name") String name, @JsonProperty("creationDate") Date creationDate, @JsonProperty("ttlMillis") Long ttlMillis) {
		super(name, creationDate, ttlMillis);
	}

	@JsonIgnore
    public abstract Date getLastUpdateDate();
}

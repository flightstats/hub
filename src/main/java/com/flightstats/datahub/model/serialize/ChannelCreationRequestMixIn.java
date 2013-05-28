package com.flightstats.datahub.model.serialize;

import org.codehaus.jackson.annotate.JsonProperty;

public abstract class ChannelCreationRequestMixIn {

	public ChannelCreationRequestMixIn(@JsonProperty("name") String name) {
		throw new IllegalStateException("Do not instantiate mix-in configuration classes.");
	}
}

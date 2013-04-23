package com.flightstats.datahub.model.serialize;

import org.codehaus.jackson.annotate.JsonProperty;

import java.util.Date;

public abstract class MetadataResponseMixIn {

	@JsonProperty("name")
	public String getName() {
		throw new IllegalStateException("Mix-in methods should not be invoked.");
	}

	@JsonProperty("creationDate")
	public Date getCreationDate() {
		throw new IllegalStateException("Mix-in methods should not be invoked.");
	}

	@JsonProperty("lastUpdateDate")
	public Date getLastUpdateDate() {
		throw new IllegalStateException("Mix-in methods should not be invoked.");
	}

}

package com.flightstats.datahub.model.serialize;

import org.codehaus.jackson.annotate.JsonProperty;

import java.util.Date;

public abstract class MetadataResponseMixIn {

	@JsonProperty("name")
	abstract public String getName();

	@JsonProperty("creationDate")
	abstract public Date getCreationDate();

	@JsonProperty("lastUpdateDate")
	abstract public Date getLastUpdateDate();

}

package com.flightstats.datahub.model.serialize;

import com.flightstats.datahub.model.ChannelConfiguration;
import com.flightstats.datahub.model.MetadataResponse;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.Date;

public abstract class MetadataResponseMixIn extends MetadataResponse {

	public MetadataResponseMixIn(ChannelConfiguration config, Date lastUpdateDate) {
		super(config, lastUpdateDate);
	}

	@JsonProperty("name")
	abstract public String getName();

	@JsonProperty("creationDate")
	abstract public Date getCreationDate();

	@JsonProperty("lastUpdateDate")
	abstract public Date getLastUpdateDate();

	@JsonProperty("ttlMillis")
	abstract public Long getTtlMillis();

}

package com.flightstats.datahub.model.serialize;

import com.flightstats.datahub.model.ChannelCreationRequest;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.map.exc.UnrecognizedPropertyException;

import java.util.Map;

public abstract class ChannelCreationRequestMixIn extends ChannelCreationRequest {

	protected ChannelCreationRequestMixIn(Builder builder) {
		super(builder);
	}

	@JsonCreator
	protected static ChannelCreationRequest create(Map<String, String> props) throws UnrecognizedPropertyException {
		return null;
	}
}

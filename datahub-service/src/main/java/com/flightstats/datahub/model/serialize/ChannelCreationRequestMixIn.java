package com.flightstats.datahub.model.serialize;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.flightstats.datahub.model.ChannelCreationRequest;
import com.flightstats.jackson.AbstractMixIn;

import java.util.Map;

@AbstractMixIn
public abstract class ChannelCreationRequestMixIn extends ChannelCreationRequest {

	protected ChannelCreationRequestMixIn(Builder builder) {
		super(builder);
	}

	@JsonCreator
	protected static ChannelCreationRequest create(Map<String, String> props) throws UnrecognizedPropertyException {
		return null;
	}
}

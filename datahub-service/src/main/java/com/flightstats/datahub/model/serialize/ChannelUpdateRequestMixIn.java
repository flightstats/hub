package com.flightstats.datahub.model.serialize;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.flightstats.datahub.model.ChannelUpdateRequest;
import com.flightstats.jackson.AbstractMixIn;

import java.util.Map;

@SuppressWarnings({"UnusedDeclaration", "MethodOverridesStaticMethodOfSuperclass"})
@AbstractMixIn
public abstract class ChannelUpdateRequestMixIn extends ChannelUpdateRequest {

	protected ChannelUpdateRequestMixIn(Builder builder) {
		super(builder);
	}

	@JsonCreator
	protected static ChannelUpdateRequest create(Map<String, String> props) throws UnrecognizedPropertyException {
		return null;
	}
}

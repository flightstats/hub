package com.flightstats.datahub.model.serialize;

import com.flightstats.datahub.model.ChannelUpdateRequest;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.map.exc.UnrecognizedPropertyException;

import java.util.Map;

@SuppressWarnings({"UnusedDeclaration", "MethodOverridesStaticMethodOfSuperclass"})
public abstract class ChannelUpdateRequestMixIn extends ChannelUpdateRequest {

	protected ChannelUpdateRequestMixIn(Builder builder) {
		super(builder);
	}

	@JsonCreator
	protected static ChannelUpdateRequest create(Map<String, String> props) throws UnrecognizedPropertyException {
		return null;
	}
}

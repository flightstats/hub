package com.flightstats.datahub.app.config.metrics;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface PerChannelTimed {
	/**
	 * Which path parameter contains the channel name?
	 */
	String channelNamePathParameter();

	/**
	 * What is this method doing?  Inserting?  Fetching?  Deleting?  Some other verb...
	 */
	String operationName();
}

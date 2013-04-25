package com.flightstats.datahub.dao;

import com.flightstats.datahub.model.exception.NoSuchChannelException;

public class CassandraUtils {

	public static RuntimeException maybePromoteToNoSuchChannel(RuntimeException e, String channelName) {
		if (exceptionWasCausedByMissingChannel(e)) {
			return new NoSuchChannelException("Channel does not exist: " + channelName, e);
		}
		return e;
	}

	private static boolean exceptionWasCausedByMissingChannel(Exception e) {
		return e.getMessage().contains("unconfigured columnfamily");
	}
}

package com.flightstats.datahub.cluster;

import java.util.concurrent.locks.Lock;

public interface ChannelLockFactory {

	/** Get the existing lock for the channel name, creating a new one if one doesn't exist. */
	Lock getLock(String channelName);
}

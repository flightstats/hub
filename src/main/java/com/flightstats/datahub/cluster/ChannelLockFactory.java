package com.flightstats.datahub.cluster;

import java.util.concurrent.locks.Lock;

public interface ChannelLockFactory {

	Lock newLock(String channelName);
}

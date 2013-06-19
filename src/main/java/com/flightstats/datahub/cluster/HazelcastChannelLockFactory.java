package com.flightstats.datahub.cluster;

import com.google.inject.Inject;
import com.hazelcast.core.HazelcastInstance;

import java.util.concurrent.locks.Lock;

public class HazelcastChannelLockFactory implements ChannelLockFactory {

	private final HazelcastInstance hazelcast;

	@Inject
	public HazelcastChannelLockFactory(HazelcastInstance hazelcast) {
		this.hazelcast = hazelcast;
	}

	@Override
	public Lock newLock(String channelName) {
		String lockName = "CHANNEL_LOCK:" + channelName;
		return hazelcast.getLock(lockName);

	}
}

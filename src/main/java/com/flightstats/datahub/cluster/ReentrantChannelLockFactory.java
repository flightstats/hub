package com.flightstats.datahub.cluster;

import com.google.common.annotations.VisibleForTesting;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ReentrantChannelLockFactory implements ChannelLockFactory {

	private final ConcurrentMap<String, Lock> channelLocks;

	public ReentrantChannelLockFactory() {
		this(new ConcurrentHashMap<String, Lock>());
	}

	@VisibleForTesting
	public ReentrantChannelLockFactory(ConcurrentHashMap<String, Lock> locks) {
		channelLocks = locks;
	}

	@Override
	public Lock getLock(String channelName) {
		ReentrantLock newLock = new ReentrantLock();
		Lock existingLock = channelLocks.putIfAbsent(channelName, newLock);
		return existingLock == null ? newLock : existingLock;
	}
}

package com.flightstats.datahub.service;

import com.flightstats.datahub.cluster.ChannelLockFactory;
import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;

import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;

public class ChannelLockExecutor {

	private final ConcurrentMap<String, Lock> channelLocks;
	private final ChannelLockFactory channelLockFactory;

	@Inject
	public ChannelLockExecutor(ChannelLockFactory channelLockFactory) {
		this(new ConcurrentHashMap<String, Lock>(), channelLockFactory);
	}

	/**
	 * This constructor only exists for testing and should not be used by production code.
	 */
	@VisibleForTesting
	ChannelLockExecutor(ConcurrentMap<String, Lock> channelLocks, ChannelLockFactory channelLockFactory) {
		this.channelLocks = channelLocks;
		this.channelLockFactory = channelLockFactory;
	}

	public <T> T execute(String channelName, Callable<T> callable) throws Exception {
		Lock lock = getLock(channelName);
		lock.lock();
		try {
			return callable.call();
		} finally {
			lock.unlock();
		}
	}

	private Lock getLock(String channelName) {
		Lock newLock = channelLockFactory.newLock(channelName);

		// CRK - what's the point of the lock map if we always first get the lock from the factory anyway?
		Lock existingLock = channelLocks.putIfAbsent(channelName, newLock);
		return existingLock == null ? newLock : existingLock;
	}
}

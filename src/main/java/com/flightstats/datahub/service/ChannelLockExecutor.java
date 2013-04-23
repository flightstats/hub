package com.flightstats.datahub.service;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Singleton;

import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Singleton
public class ChannelLockExecutor {

	private final ConcurrentMap<String, Lock> channelLocks;

	public ChannelLockExecutor() {
		this(new ConcurrentHashMap<String, Lock>());
	}

	/**
	 * This constructor only exists for testing and should not be used by production code.
	 */
	@VisibleForTesting
	ChannelLockExecutor(ConcurrentMap<String, Lock> channelLocks) {
		this.channelLocks = channelLocks;
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
		ReentrantLock newLock = new ReentrantLock();
		Lock existingLock = channelLocks.putIfAbsent(channelName, newLock);
		return existingLock == null ? newLock : existingLock;
	}
}

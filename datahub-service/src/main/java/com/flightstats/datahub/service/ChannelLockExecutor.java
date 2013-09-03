package com.flightstats.datahub.service;

import com.flightstats.datahub.cluster.ChannelLockFactory;
import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;

import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;

public class ChannelLockExecutor {

	private final ChannelLockFactory channelLockFactory;

	@Inject
	public ChannelLockExecutor(ChannelLockFactory channelLockFactory) {
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
		return channelLockFactory.getLock(channelName);
	}
}

package com.flightstats.datahub.cluster;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ReentrantChannelLockFactory implements ChannelLockFactory {

	@Override
	public Lock newLock(String channelName) {
		return new ReentrantLock();

	}
}

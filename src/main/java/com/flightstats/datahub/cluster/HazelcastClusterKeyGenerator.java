package com.flightstats.datahub.cluster;

import com.flightstats.datahub.model.DataHubKey;
import com.flightstats.datahub.service.ChannelLockExecutor;
import com.flightstats.datahub.util.DataHubKeyGenerator;
import com.flightstats.datahub.util.TimeProvider;
import com.google.inject.Inject;
import com.hazelcast.core.AtomicNumber;
import com.hazelcast.core.HazelcastInstance;

import java.util.Date;
import java.util.concurrent.Callable;

public class HazelcastClusterKeyGenerator implements DataHubKeyGenerator {

	private final TimeProvider timeProvider;
	private final HazelcastInstance hazelcastInstance;
	private final ChannelLockExecutor channelLockExecutor;

	@Inject
	public HazelcastClusterKeyGenerator(TimeProvider timeProvider, HazelcastInstance hazelcastInstance, ChannelLockExecutor channelLockExecutor) {
		this.timeProvider = timeProvider;
		this.hazelcastInstance = hazelcastInstance;
		this.channelLockExecutor = channelLockExecutor;
	}

	@Override
	public DataHubKey newKey(final String channelName) {
		try {
			KeyGeneratingCallable keyGeneratingCallable = new KeyGeneratingCallable(channelName);
			return channelLockExecutor.execute(channelName, keyGeneratingCallable);
		} catch (Exception e) {
			throw new RuntimeException("Error generating new DataHubKey: ", e);
		}
	}

	private class KeyGeneratingCallable implements Callable<DataHubKey> {
		private final String channelName;

		public KeyGeneratingCallable(String channelName) {
			this.channelName = channelName;
		}

		@Override
		public DataHubKey call() throws Exception {
			Date currentDate = timeProvider.getDate();
			AtomicNumber atomicDate = hazelcastInstance.getAtomicNumber("CHANNEL_NAME_DATE:" + channelName);
			Date lastDate = new Date(atomicDate.get());

			if (currentDate.compareTo(lastDate) <= 0) {  //in the same millisecond or before in time
				long sequence = hazelcastInstance.getAtomicNumber("CHANNEL_NAME_SEQ:" + channelName).getAndAdd(1);
				return new DataHubKey(lastDate, (short) sequence);
			}
			atomicDate.set(currentDate.getTime());
			return new DataHubKey(currentDate, (short) 0);
		}
	}
}

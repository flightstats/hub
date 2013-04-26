package com.flightstats.datahub.dao.prototypes;

import com.flightstats.datahub.dao.ChannelDao;
import com.flightstats.datahub.model.*;
import com.flightstats.datahub.util.TimeProvider;
import com.google.common.base.Optional;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Maps;
import com.google.inject.Inject;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class InMemoryChannelDao implements ChannelDao {
	private final TimeProvider timeProvider;

	private final Map<String, ChannelConfiguration> channelConfigurations = Maps.newConcurrentMap();
	private final Map<String, Lock> writeLocks = Maps.newConcurrentMap();
	private final Map<String, DataHubChannelValueKey> latestPerChannel = Maps.newConcurrentMap();
	private final Cache<DataHubChannelValueKey, LinkedDataHubCompositeValue> channelValues = CacheBuilder.newBuilder().expireAfterWrite(1, TimeUnit.HOURS).build();

	@Inject
	public InMemoryChannelDao(TimeProvider timeProvider) {
		this.timeProvider = timeProvider;
	}

	@Override
	public boolean channelExists(String channelName) {
		return channelConfigurations.containsKey(channelName);
	}

	@Override
	public ChannelConfiguration createChannel(String name) {
		Date creationDate = timeProvider.getDate();
		ChannelConfiguration channelConfiguration = new ChannelConfiguration(name, creationDate);
		writeLocks.put(name, new ReentrantLock());
		channelConfigurations.put(name, channelConfiguration);
		return channelConfiguration;
	}

	@Override
	public ChannelConfiguration getChannelConfiguration(String channelName) {
		return channelConfigurations.get(channelName);
	}

	@Override
	public int countChannels() {
		return channelConfigurations.size();
	}

	@Override
	public ValueInsertionResult insert(String channelName, String contentType, byte[] data) {
		Lock lock = writeLocks.get(channelName);
		lock.lock();
		try {
			DataHubChannelValueKey oldLastKey = latestPerChannel.get(channelName);
			short newSequence = (oldLastKey == null) ? ((short) 0) : (short) (oldLastKey.sequence + 1);
			DataHubKey newKey = new DataHubKey(timeProvider.getDate(), newSequence);
			DataHubChannelValueKey newDataHubChannelValueKey = new DataHubChannelValueKey(newKey, channelName);
			DataHubCompositeValue dataHubCompositeValue = new DataHubCompositeValue(contentType, data);
			LinkedDataHubCompositeValue newLinkedValue = new LinkedDataHubCompositeValue(dataHubCompositeValue, optionalFromCompositeKey(oldLastKey), Optional.<DataHubKey>absent());

			//note: the order of operations here is actually fairly significant, to avoid races, so I'm calling it out explicitly with comments.
			//first put the actual value in.
			channelValues.put(newDataHubChannelValueKey, newLinkedValue);
			//then link the old previous to the new value
			linkOldPreviousToNew(oldLastKey, newDataHubChannelValueKey);
			//finally, make it the latest
			latestPerChannel.put(channelName, newDataHubChannelValueKey);

			return new ValueInsertionResult(newKey);
		} finally {
			lock.unlock();
		}
	}

	private Optional<DataHubKey> optionalFromCompositeKey(DataHubChannelValueKey key) {
		if (key != null) {
			return Optional.of(new DataHubKey(key.date, key.sequence));
		}
		return Optional.absent();
	}

	private void linkOldPreviousToNew(DataHubChannelValueKey oldLastKey, DataHubChannelValueKey newKey) {
		if (oldLastKey != null) {
			LinkedDataHubCompositeValue previousLinkedValue = channelValues.getIfPresent(oldLastKey);
			//in case the value has expired.
			if (previousLinkedValue != null) {
				channelValues.put(oldLastKey, new LinkedDataHubCompositeValue(previousLinkedValue.getValue(), previousLinkedValue.getPrevious(), Optional.of(new DataHubKey(newKey.date, newKey.sequence))));
			}
		}
	}

	@Override
	public Optional<LinkedDataHubCompositeValue> getValue(String channelName, DataHubKey key) {
		return Optional.fromNullable(channelValues.getIfPresent(new DataHubChannelValueKey(key, channelName)));
	}

	@Override
	public Optional<DataHubKey> findLatestId(String channelName) {
		DataHubChannelValueKey key = latestPerChannel.get(channelName);
		return optionalFromCompositeKey(key);
	}

	private static class DataHubChannelValueKey {
		private final Date date;
		private final short sequence;
		private final String channelName;

		private DataHubChannelValueKey(DataHubKey dataHubKey, String channelName) {
			this.date = dataHubKey.getDate();
			this.sequence = dataHubKey.getSequence();
			this.channelName = channelName;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			DataHubChannelValueKey that = (DataHubChannelValueKey) o;

			if (sequence != that.sequence) return false;
			if (!channelName.equals(that.channelName)) return false;
			if (!date.equals(that.date)) return false;

			return true;
		}

		@Override
		public int hashCode() {
			int result = date.hashCode();
			result = 31 * result + sequence;
			result = 31 * result + channelName.hashCode();
			return result;
		}

		@Override
		public String toString() {
			return "DataHubChannelValueKey{" +
					"date=" + date +
					", sequence=" + sequence +
					", channelName='" + channelName + '\'' +
					'}';
		}
	}

}

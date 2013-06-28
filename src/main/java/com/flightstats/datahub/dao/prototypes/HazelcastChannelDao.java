package com.flightstats.datahub.dao.prototypes;

import com.flightstats.datahub.dao.ChannelDao;
import com.flightstats.datahub.model.*;
import com.google.common.base.Optional;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;

public class HazelcastChannelDao implements ChannelDao {

	public static final HazelcastInstance HAZELCAST_INSTANCE = Hazelcast.newHazelcastInstance();

	private final Map<String, ChannelConfiguration> channelConfigurations = HAZELCAST_INSTANCE.getMap("channelConfigurations");
	private final Map<String, DataHubKey> latestPerChannel = HAZELCAST_INSTANCE.getMap("latestPerChannel");
	private final ConcurrentMap<String, DataHubKey> firstPerChannel = HAZELCAST_INSTANCE.getMap("firstPerChannel");
	private final Map<DataHubKey, LinkedDataHubCompositeValue> channelValues = HAZELCAST_INSTANCE.getMap("channelValues");

	@Override
	public boolean channelExists(String channelName) {
		return channelConfigurations.containsKey(channelName);
	}

	@Override
	public ChannelConfiguration createChannel(String name, Long ttl) {
		Date creationDate = new Date();
		ChannelConfiguration channelConfiguration = new ChannelConfiguration(name, creationDate, ttl);
		channelConfigurations.put(name, channelConfiguration);
		return channelConfiguration;
	}

	@Override
	public ChannelConfiguration getChannelConfiguration(String channelName) {
		return channelConfigurations.get(channelName);
	}

	@Override
	public Iterable<ChannelConfiguration> getChannels() {
		return Collections.unmodifiableCollection(channelConfigurations.values());
	}

	@Override
	public int countChannels() {
		return channelConfigurations.size();
	}

	@Override
	public void setFirstKey(String channelName, DataHubKey key) {
		firstPerChannel.putIfAbsent(channelName, key);
	}

	@Override
	public void deleteFirstKey(String channelName) {
		firstPerChannel.remove(channelName);
	}

	@Override
	public void setLastUpdateKey(String channelName, DataHubKey key) {
		latestPerChannel.put(channelName, key);
	}

	@Override
	public void deleteLastUpdateKey(String channelName) {
		latestPerChannel.remove(channelName);
	}

	@Override
	public void delete(String channelName, List<DataHubKey> keys) {
		for (DataHubKey reapableKey : keys) {
			channelValues.remove(reapableKey);
		}
	}

	@Override
	public Collection<DataHubKey> findKeysInRange(String channelName, Date startTime, Date endTime) {
		throw new UnsupportedOperationException();
	}

	@Override
	public ValueInsertionResult insert(String channelName, String contentType, byte[] data) {
		Lock lock = HAZELCAST_INSTANCE.getLock(channelName + "-writeLock");
		lock.lock();
		try {
			DataHubKey oldLastKey = latestPerChannel.get(channelName);
			short newSequence = (oldLastKey == null) ? ((short) 0) : (short) (oldLastKey.getSequence() + 1);
			DataHubKey newKey = new DataHubKey(new Date(), newSequence);

			DataHubCompositeValue dataHubCompositeValue = new DataHubCompositeValue(contentType, data);
			LinkedDataHubCompositeValue newLinkedValue = new LinkedDataHubCompositeValue(dataHubCompositeValue, Optional.fromNullable(oldLastKey),
					Optional.<DataHubKey>absent());
			//first put the actual value in.
			channelValues.put(newKey, newLinkedValue);
			//then link the old previous to the new value
			if (oldLastKey != null) {
				LinkedDataHubCompositeValue previousLinkedValue = channelValues.get(oldLastKey);
				channelValues.put(oldLastKey,
						new LinkedDataHubCompositeValue(previousLinkedValue.getValue(), previousLinkedValue.getPrevious(), Optional.of(newKey)));
			}
			//finally, make it the latest
			setLastUpdateKey(channelName, newKey);
			setFirstKey(channelName, newKey);
			return new ValueInsertionResult(newKey);
		} finally {
			lock.unlock();
		}
	}

	@Override
	public Optional<LinkedDataHubCompositeValue> getValue(String channelName, DataHubKey key) {
		return Optional.of(channelValues.get(key));
	}

	@Override
	public Optional<DataHubKey> findFirstUpdateKey(String channelName) {
		DataHubKey key = firstPerChannel.get(channelName);
		return Optional.fromNullable(key);
	}

	@Override
	public Optional<DataHubKey> findLastUpdatedKey(String channelName) {
		DataHubKey key = latestPerChannel.get(channelName);
		return Optional.fromNullable(key);
	}
}

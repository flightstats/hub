package com.flightstats.datahub.dao.prototypes;

import com.flightstats.datahub.dao.ChannelDao;
import com.flightstats.datahub.model.*;
import com.google.common.base.Optional;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

import java.util.Collections;
import java.util.Date;
import java.util.Map;
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
	public ChannelConfiguration createChannel(String name) {
		Date creationDate = new Date();
		ChannelConfiguration channelConfiguration = new ChannelConfiguration(name, creationDate);
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
			latestPerChannel.put(channelName, newKey);
			firstPerChannel.putIfAbsent(channelName,newKey);
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
	public Optional<DataHubKey> findFirstId(String channelName) {
		DataHubKey key = firstPerChannel.get(channelName);
		return Optional.fromNullable(key);
	}

	@Override
	public Optional<DataHubKey> findLatestId(String channelName) {
		DataHubKey key = latestPerChannel.get(channelName);
		return Optional.fromNullable(key);
	}
}

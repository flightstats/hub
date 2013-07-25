package com.flightstats.datahub.dao.memory;

import com.flightstats.datahub.dao.ChannelDao;
import com.flightstats.datahub.model.*;
import com.flightstats.datahub.model.exception.NoSuchChannelException;
import com.flightstats.datahub.util.TimeProvider;
import com.google.common.base.Optional;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Maps;
import com.google.inject.Inject;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class InMemoryChannelDao implements ChannelDao {
	private final TimeProvider timeProvider;

	private final Map<String, ChannelConfiguration> channelConfigurations = Maps.newConcurrentMap();
	private final Map<String, DataHubChannelValueKey> latestPerChannel = Maps.newConcurrentMap();
	private final Map<String, DataHubChannelValueKey> firstPerChannel = Maps.newConcurrentMap();
	private final Cache<DataHubChannelValueKey, LinkedDataHubCompositeValue> channelValues = CacheBuilder.newBuilder()
																										 .expireAfterWrite(1, TimeUnit.HOURS)
																										 .build();

	@Inject
	public InMemoryChannelDao(TimeProvider timeProvider) {
		this.timeProvider = timeProvider;
	}

	@Override
	public boolean channelExists(String channelName) {
		return channelConfigurations.containsKey(channelName);
	}

	@Override
	public ChannelConfiguration createChannel(String name, Long ttlMillis) {
		Date creationDate = timeProvider.getDate();
		ChannelConfiguration channelConfiguration = new ChannelConfiguration(name, creationDate, ttlMillis);
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
	public void delete(String channelName, List<DataHubKey> keys) {
		channelValues.invalidateAll(keys);
	}

	@Override
	public Collection<DataHubKey> findKeysInRange(String channelName, Date startTime, Date endTime) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void updateChannelMetadata(ChannelConfiguration newConfig) {
		channelConfigurations.put(newConfig.getName(), newConfig);
	}

	@Override
	public void setLastUpdateKey(String channelName, DataHubKey key) {
		latestPerChannel.put(channelName, new DataHubChannelValueKey(key, channelName));
	}

	@Override
	public void deleteLastUpdateKey(String channelName) {
		latestPerChannel.remove(channelName);
	}

	@Override
	public void setFirstKey(String channelName, DataHubKey key) {
		firstPerChannel.put(channelName, new DataHubChannelValueKey(key, channelName));
	}

	@Override
	public void deleteFirstKey(String channelName) {
		firstPerChannel.remove(channelName);
	}

	@Override
	public ValueInsertionResult insert(String channelName, Optional<String> contentType, Optional<String> contentEncoding, Optional<String> contentLanguage, byte[] data) {
		if (!channelExists(channelName)) {
			throw new NoSuchChannelException("No such channel: " + channelName, new RuntimeException());
		}
		DataHubChannelValueKey oldLastKey = latestPerChannel.get(channelName);
		short newSequence = (oldLastKey == null) ? ((short) 0) : (short) (oldLastKey.getSequence() + 1);
		DataHubKey newKey = new DataHubKey(timeProvider.getDate(), newSequence);
		DataHubChannelValueKey newDataHubChannelValueKey = new DataHubChannelValueKey(newKey, channelName);
		DataHubCompositeValue dataHubCompositeValue = new DataHubCompositeValue(contentType, contentEncoding, contentLanguage, data);
		LinkedDataHubCompositeValue newLinkedValue = new LinkedDataHubCompositeValue(dataHubCompositeValue, optionalFromCompositeKey(oldLastKey),
				Optional.<DataHubKey>absent());

		//note: the order of operations here is actually fairly significant, to avoid races, so I'm calling it out explicitly with comments.
		//first put the actual value in.
		channelValues.put(newDataHubChannelValueKey, newLinkedValue);
		//then link the old previous to the new value
		linkOldPreviousToNew(oldLastKey, newDataHubChannelValueKey);
		//finally, make it the latest
		setLastUpdateKey(channelName, newDataHubChannelValueKey.asDataHubKey());
		if (!firstPerChannel.containsKey(channelName)) {
			setFirstKey(channelName, newDataHubChannelValueKey.asDataHubKey());
		}

		return new ValueInsertionResult(newKey);
	}

	private Optional<DataHubKey> optionalFromCompositeKey(DataHubChannelValueKey key) {
		if (key != null) {
			return Optional.of(key.asDataHubKey());
		}
		return Optional.absent();
	}

	private void linkOldPreviousToNew(DataHubChannelValueKey oldLastKey, DataHubChannelValueKey newKey) {
		if (oldLastKey != null) {
			LinkedDataHubCompositeValue previousLinkedValue = channelValues.getIfPresent(oldLastKey);
			//in case the value has expired.
			if (previousLinkedValue != null) {
				channelValues.put(oldLastKey, new LinkedDataHubCompositeValue(previousLinkedValue.getValue(), previousLinkedValue.getPrevious(),
						Optional.of(newKey.asDataHubKey())));
			}
		}
	}

	@Override
	public Optional<LinkedDataHubCompositeValue> getValue(String channelName, DataHubKey key) {
		return Optional.fromNullable(channelValues.getIfPresent(new DataHubChannelValueKey(key, channelName)));
	}

	@Override
	public Optional<DataHubKey> findFirstUpdateKey(String channelName) {
		DataHubChannelValueKey key = firstPerChannel.get(channelName);
		return optionalFromCompositeKey(key);
	}

	@Override
	public Optional<DataHubKey> findLastUpdatedKey(String channelName) {
		DataHubChannelValueKey key = latestPerChannel.get(channelName);
		return optionalFromCompositeKey(key);
	}

}

package com.flightstats.datahub.dao;

import com.flightstats.datahub.model.*;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.List;

public class CassandraChannelDao implements ChannelDao {

	private final static Logger logger = LoggerFactory.getLogger(ChannelDao.class);
	private final CassandraChannelsCollection channelsCollection;
	private final CassandraValueWriter cassandraValueWriter;
	private final CassandraValueReader cassandraValueReader;
	private final CassandraLinkagesFinder linkagesFinder;

	@Inject
	public CassandraChannelDao(CassandraChannelsCollection channelsCollection, CassandraValueWriter cassandraValueWriter, CassandraValueReader cassandraValueReader, CassandraLinkagesFinder linkagesFinder) {
		this.channelsCollection = channelsCollection;
		this.cassandraValueWriter = cassandraValueWriter;
		this.cassandraValueReader = cassandraValueReader;
		this.linkagesFinder = linkagesFinder;
	}

	@Override
	public boolean channelExists(String channelName) {
		return channelsCollection.channelExists(channelName);
	}

	@Override
	public ChannelConfiguration createChannel(String name, Long ttl) {
		logger.info("Creating channel name = " + name + ", with ttl = " + ttl);
		return channelsCollection.createChannel(name, ttl);
	}

	@Override
	public ValueInsertionResult insert(String channelName, String contentType, byte[] data) {
		logger.debug("Inserting " + data.length + " bytes of type " + contentType + " into channel " + channelName);
		DataHubCompositeValue value = new DataHubCompositeValue(contentType, data);
		ValueInsertionResult result = cassandraValueWriter.write(channelName, value);
		setLastUpdateKey(channelName, result.getKey());
		if ( !findFirstId(channelName).isPresent() ) {
			setFirstKey(channelName, result.getKey());
		}
		return result;
	}

	@Override
	public void delete(String channelName, List<DataHubKey> keys) {
		cassandraValueWriter.delete(channelName, keys);
	}

	@Override
	public void setLastUpdateKey(String channelName, DataHubKey result) {
		channelsCollection.updateLastUpdatedKey(channelName, result);
	}

	@Override
	public void deleteLastUpdateKey(String channelName) {
		Optional<DataHubKey> latestId = findLatestId(channelName);
		if ( latestId.isPresent() ) {
			channelsCollection.deleteLastUpdatedKey(channelName, latestId.get());
		}
	}

	@Override
	public void setFirstKey(String channelName, DataHubKey result) {
		channelsCollection.updateFirstKey(channelName, result);
	}

	@Override
	public void deleteFirstKey(String channelName) {
		Optional<DataHubKey> firstId = findFirstId(channelName);
		if ( firstId.isPresent() ) {
			channelsCollection.deleteFirstKey(channelName, firstId.get());
		}
	}

	@Override
	public Optional<LinkedDataHubCompositeValue> getValue(String channelName, DataHubKey key) {
		logger.debug("Fetching " + key.toString() + " from channel " + channelName);
		DataHubCompositeValue value = cassandraValueReader.read(channelName, key);
		if (value == null) {
			return Optional.absent();
		}
		Optional<DataHubKey> previous = linkagesFinder.findPrevious(channelName, key);
		Optional<DataHubKey> next = linkagesFinder.findNext(channelName, key);
		return Optional.of(new LinkedDataHubCompositeValue(value, previous, next));
	}

	@Override
	public ChannelConfiguration getChannelConfiguration(String channelName) {
		return channelsCollection.getChannelConfiguration(channelName);
	}

	@Override
	public Iterable<ChannelConfiguration> getChannels() {
		return channelsCollection.getChannels();
	}

	@Override
	public Optional<DataHubKey> findFirstId(String channelName) {
		return cassandraValueReader.findFirstId(channelName);
	}

	@Override
	public Optional<DataHubKey> findLatestId(String channelName) {
		return cassandraValueReader.findLatestId(channelName);
	}

	@Override
	public int countChannels() {
		return channelsCollection.countChannels();
	}
}

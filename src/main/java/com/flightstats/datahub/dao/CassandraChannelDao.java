package com.flightstats.datahub.dao;

import com.flightstats.datahub.model.*;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import me.prettyprint.hector.api.exceptions.HInvalidRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static com.flightstats.datahub.dao.CassandraUtils.maybePromoteToNoSuchChannel;

public class CassandraChannelDao implements ChannelDao {

	private final static Logger logger = LoggerFactory.getLogger(ChannelDao.class);
	private final CassandraChannelsCollection channelsCollection;
	private final CassandraLinkagesCollection linkagesCollection;
	private final CassandraValueWriter cassandraValueWriter;
	private final CassandraValueReader cassandraValueReader;

	@Inject
	public CassandraChannelDao(CassandraChannelsCollection channelsCollection, CassandraLinkagesCollection linkagesCollection, CassandraValueWriter cassandraValueWriter, CassandraValueReader cassandraValueReader) {
		this.channelsCollection = channelsCollection;
		this.linkagesCollection = linkagesCollection;
		this.cassandraValueWriter = cassandraValueWriter;
		this.cassandraValueReader = cassandraValueReader;
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
		DataHubKey insertedKey = result.getKey();

		// Note:  Caching the latest could greatly speed up writes, but this has to be done in a distributed fashion.
		DataHubKey lastUpdatedKey = channelsCollection.getLastUpdatedKey(channelName);

		updateFirstAndLastKeysForChannel(channelName, insertedKey);

		linkagesCollection.updateLinkages(channelName, insertedKey, lastUpdatedKey);

		return result;
	}

	private void updateFirstAndLastKeysForChannel(String channelName, DataHubKey insertedKey) {
		setLastUpdateKey(channelName, insertedKey);
		if (!findFirstUpdateKey(channelName).isPresent()) {
			setFirstKey(channelName, insertedKey);
		}
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
		Optional<DataHubKey> latestId = findLastUpdatedKey(channelName);
		if (latestId.isPresent()) {
			channelsCollection.deleteLastUpdatedKey(channelName);
		}
	}

	@Override
	public void setFirstKey(String channelName, DataHubKey result) {
		channelsCollection.updateFirstKey(channelName, result);
	}

	@Override
	public void deleteFirstKey(String channelName) {
		Optional<DataHubKey> firstId = findFirstUpdateKey(channelName);
		if (firstId.isPresent()) {
			channelsCollection.deleteFirstKey(channelName);
		}
	}

	@Override
	public Optional<LinkedDataHubCompositeValue> getValue(String channelName, DataHubKey key) {
		logger.debug("Fetching " + key.toString() + " from channel " + channelName);
		DataHubCompositeValue value = cassandraValueReader.read(channelName, key);
		if (value == null) {
			return Optional.absent();
		}
		Optional<DataHubKey> previous = linkagesCollection.findPreviousKey(channelName, key);
		Optional<DataHubKey> next = linkagesCollection.findNextKey(channelName, key);
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
	public Optional<DataHubKey> findFirstUpdateKey(String channelName) {
		try {
			DataHubKey firstKey = channelsCollection.getFirstKey(channelName);
			return Optional.fromNullable(firstKey);
		} catch (HInvalidRequestException e) {
			throw maybePromoteToNoSuchChannel(e, channelName);
		}
	}

	@Override
	public Optional<DataHubKey> findLastUpdatedKey(String channelName) {
		try {
			DataHubKey lastUpdatedKey = channelsCollection.getLastUpdatedKey(channelName);
			return Optional.fromNullable(lastUpdatedKey);
		} catch (HInvalidRequestException e) {
			throw maybePromoteToNoSuchChannel(e, channelName);
		}
	}

	@Override
	public int countChannels() {
		return channelsCollection.countChannels();
	}
}

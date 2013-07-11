package com.flightstats.datahub.dao;

import com.flightstats.datahub.dao.serialize.DataHubCompositeValueSerializer;
import com.flightstats.datahub.model.*;
import com.flightstats.datahub.util.DataHubKeyRenderer;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.beans.OrderedRows;
import me.prettyprint.hector.api.beans.Row;
import me.prettyprint.hector.api.exceptions.HInvalidRequestException;
import me.prettyprint.hector.api.query.QueryResult;
import me.prettyprint.hector.api.query.RangeSlicesQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

import static com.flightstats.datahub.dao.CassandraUtils.maybePromoteToNoSuchChannel;

public class CassandraChannelDao implements ChannelDao {

	private final static Logger logger = LoggerFactory.getLogger(ChannelDao.class);

	private final CassandraChannelsCollection channelsCollection;
	private final CassandraLinkagesCollection linkagesCollection;
	private final CassandraValueWriter cassandraValueWriter;
	private final CassandraValueReader cassandraValueReader;
	private final DataHubKeyRenderer keyRenderer;
	private final RowKeyStrategy<String, DataHubKey, DataHubCompositeValue> rowKeyStrategy;
	private final CassandraConnector connector;
	private final HectorFactoryWrapper hector;
	private final ConcurrentMap<String,DataHubKey> lastUpdatedPerChannel;

	@Inject
	public CassandraChannelDao(
		CassandraChannelsCollection channelsCollection, CassandraLinkagesCollection linkagesCollection,
		CassandraValueWriter cassandraValueWriter, CassandraValueReader cassandraValueReader,
		DataHubKeyRenderer keyRenderer, RowKeyStrategy<String, DataHubKey, DataHubCompositeValue> rowKeyStrategy,
		CassandraConnector connector, HectorFactoryWrapper hector,
		@Named("LastUpdatePerChannelMap") ConcurrentMap<String,DataHubKey> lastUpdatedPerChannel) {
		this.channelsCollection = channelsCollection;
		this.linkagesCollection = linkagesCollection;
		this.cassandraValueWriter = cassandraValueWriter;
		this.cassandraValueReader = cassandraValueReader;
		this.keyRenderer = keyRenderer;
		this.rowKeyStrategy = rowKeyStrategy;
		this.connector = connector;
		this.hector = hector;
		this.lastUpdatedPerChannel = lastUpdatedPerChannel;
	}

	@Override
	public boolean channelExists(String channelName) {
		return channelsCollection.channelExists(channelName);
	}

	@Override
	public ChannelConfiguration createChannel(String name, Long ttlMillis) {
		logger.info("Creating channel name = " + name + ", with ttlMillis = " + ttlMillis);
		return channelsCollection.createChannel(name, ttlMillis);
	}

	@Override
	public void updateChannelMetadata(ChannelConfiguration newConfig) {
		channelsCollection.updateChannel(newConfig);
	}

	@Override
	public ValueInsertionResult insert(String channelName, Optional<String> contentType, Optional<String> contentEncoding, Optional<String> contentLanguage, byte[] data) {
		logger.debug("Inserting " + data.length + " bytes of type " + contentType + " into channel " + channelName);
		DataHubCompositeValue value = new DataHubCompositeValue(contentType, contentEncoding, contentLanguage, data);

		ValueInsertionResult result = cassandraValueWriter.write(channelName, value);
		DataHubKey insertedKey = result.getKey();
		DataHubKey previousKey = updateLatestKey(channelName, insertedKey);
		updateFirstKey(channelName, insertedKey);
		linkagesCollection.updateLinkages(channelName, insertedKey, previousKey);

		return result;
	}

	private void updateFirstKey(String channelName, DataHubKey newLatestKey) {
		if (!findFirstUpdateKey(channelName).isPresent()) {
			setFirstKey(channelName, newLatestKey);
		}
	}

	/** @return the previous latest key, null if one didn't exist. */
	private DataHubKey updateLatestKey(String channelName, DataHubKey newLatestKey) {
		Optional<DataHubKey> previousLastUpdatedKey = findLastUpdatedKey(channelName);
		setLastUpdateKey(channelName, newLatestKey);
		return previousLastUpdatedKey.isPresent() ? previousLastUpdatedKey.get() : null;
	}

	@Override
	public void delete(String channelName, List<DataHubKey> keys) {
		cassandraValueWriter.delete(channelName, keys);
		linkagesCollection.delete(channelName, keys);
	}

	@Override
	public Collection<DataHubKey> findKeysInRange(String channelName, Date startTime, Date endTime) {
		QueryResult<OrderedRows<String, String, DataHubCompositeValue>> results = queryForKeysInRange(channelName, startTime, endTime);
		return buildKeysFromResults(results);
	}

	private QueryResult<OrderedRows<String, String, DataHubCompositeValue>> queryForKeysInRange(String channelName, Date startTime, Date endTime) {
		DataHubKey minKey = new DataHubKey(startTime, (short) 0);
		DataHubKey maxKey = new DataHubKey(endTime, Short.MAX_VALUE);
		String minColumnKey = keyRenderer.keyToString(minKey);
		String maxColumnKey = keyRenderer.keyToString(maxKey);
		Keyspace keyspace = connector.getKeyspace();
		String maxRowKey = rowKeyStrategy.buildKey(channelName, maxKey);
		return hector.createRangeSlicesQuery(keyspace, StringSerializer.get(), StringSerializer.get(), DataHubCompositeValueSerializer.get())
		             .setColumnFamily(channelName)
		             .setRange(minColumnKey, maxColumnKey, false, Integer.MAX_VALUE)
		             .setKeys(null, maxRowKey)
		             .execute();
	}

	private Collection<DataHubKey> buildKeysFromResults(QueryResult<OrderedRows<String, String, DataHubCompositeValue>> results) {
		Collection<DataHubKey> keys = new ArrayList<>();
		Collection<Row<String, String, DataHubCompositeValue>> dataHubKeyRows = Collections2.filter(results.get().getList(),
		                                                                                            new DataHubRowKeySelector());
		for (Row<String, String, DataHubCompositeValue> row : dataHubKeyRows) {
			keys.addAll(Collections2.transform(row.getColumnSlice().getColumns(), new KeyRenderer()));
		}
		return keys;
	}

	private class DataHubRowKeySelector implements Predicate<Row<String, String, DataHubCompositeValue>> {
		@Override
		public boolean apply(Row<String, String, DataHubCompositeValue> row) {
			return !channelsCollection.isChannelMetadataRowKey(row.getKey()) && !linkagesCollection.isLinkageRowKey(row.getKey());
		}
	}

	private class KeyRenderer implements Function<HColumn<String, DataHubCompositeValue>, DataHubKey> {
		@Override
		public DataHubKey apply(HColumn<String, DataHubCompositeValue> column) {
			return keyRenderer.fromString(column.getName());
		}
	}

	@Override
	public void setLastUpdateKey(String channelName, DataHubKey result) {
		lastUpdatedPerChannel.put(channelName, result);
	}

	@Override
	public void deleteLastUpdateKey(String channelName) {
		lastUpdatedPerChannel.remove(channelName);
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
			DataHubKey latest = getLastUpdatedFromCache(channelName);
			if (latest == null) {
				latest = queryForLatestKey(channelName);
				setLastUpdateKey(channelName, latest);
			}
			return Optional.fromNullable(latest);
		} catch (HInvalidRequestException e) {
			throw maybePromoteToNoSuchChannel(e, channelName);
		}
	}

	private DataHubKey getLastUpdatedFromCache(String channelName) {
		return lastUpdatedPerChannel.get(channelName);
	}

	@VisibleForTesting
	protected DataHubKey queryForLatestKey(String channelName) {
		Optional<DataHubKey> first = findFirstUpdateKey(channelName);
		if (!first.isPresent()) return null;

		// Hunt backwards through the rowkeys looking for the most recent column.
		String firstRowKey = rowKeyStrategy.buildKey(channelName, first.get());
		Keyspace keyspace = connector.getKeyspace();
		String currentKey = rowKeyStrategy.buildKey(channelName, new DataHubKey(new Date(), (short) 0));
		for( String rowKey = rowKeyStrategy.nextKey(channelName, currentKey);
		     rowKey.compareTo(firstRowKey) >= 0 ;
		     rowKey = rowKeyStrategy.prevKey(channelName, rowKey)) {
			RangeSlicesQuery<String,String,DataHubCompositeValue> rangeSlicesQuery = hector.createRangeSlicesQuery(
				keyspace, StringSerializer.get(), StringSerializer.get(), DataHubCompositeValueSerializer.get());
			QueryResult<OrderedRows<String, String, DataHubCompositeValue>> results =
				rangeSlicesQuery.setColumnFamily(channelName)
				                .setRange(null, null, true, 1)
				                .setKeys(rowKey, rowKey)
				                .execute();

			List<Row<String, String, DataHubCompositeValue>> rows = results.get().getList();
			if ( rows.isEmpty() ) continue;
			List<HColumn<String, DataHubCompositeValue>> columns = rows.get(0).getColumnSlice().getColumns();
			if ( columns.isEmpty() ) continue;

			return keyRenderer.fromString(columns.get(0).getName());
		}
		return null;
	}

	@Override
	public int countChannels() {
		return channelsCollection.countChannels();
	}
}

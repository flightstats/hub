package com.flightstats.datahub.dao;

import com.flightstats.datahub.model.ChannelConfiguration;
import com.flightstats.datahub.model.DataHubCompositeValue;
import com.flightstats.datahub.model.DataHubKey;
import com.flightstats.datahub.util.DataHubKeyRenderer;
import com.flightstats.datahub.util.TimeProvider;
import com.google.common.base.Strings;
import com.google.inject.Inject;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.cassandra.service.ColumnSliceIterator;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.Serializer;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.beans.OrderedRows;
import me.prettyprint.hector.api.beans.Row;
import me.prettyprint.hector.api.mutation.Mutator;
import me.prettyprint.hector.api.query.ColumnQuery;
import me.prettyprint.hector.api.query.QueryResult;
import me.prettyprint.hector.api.query.SliceQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * Encapsulates the channel creation, existence checks, and associated metadata.
 */
public class CassandraChannelsCollection {

	private final static Logger logger = LoggerFactory.getLogger(CassandraChannelsCollection.class);

	static final String CHANNELS_ROW_KEY = "DATA_HUB_CHANNELS";
	static final String CHANNELS_FIRST_ROW_KEY = "DATA_HUB_CHANNELS_FIRST";
	static final String CHANNELS_LATEST_ROW_KEY = "DATA_HUB_CHANNELS_LATEST";
	static final String CHANNELS_COLUMN_FAMILY_NAME = "channelMetadata";
	static final String MAX_CHANNEL_NAME = Strings.repeat("~", 255);

	private final CassandraConnector connector;
	private final Serializer<ChannelConfiguration> channelConfigSerializer;
	private final HectorFactoryWrapper hector;
	private final TimeProvider timeProvider;
	private final DataHubKeyRenderer keyRenderer;
	private final RowKeyStrategy<String, DataHubKey, DataHubCompositeValue> rowKeyStrategy;

	@Inject
	public CassandraChannelsCollection(CassandraConnector connector, Serializer<ChannelConfiguration> channelConfigSerializer, HectorFactoryWrapper hector, TimeProvider timeProvider, DataHubKeyRenderer keyRenderer, RowKeyStrategy<String, DataHubKey,
		DataHubCompositeValue> rowKeyStrategy) {
		this.connector = connector;
		this.channelConfigSerializer = channelConfigSerializer;
		this.hector = hector;
		this.timeProvider = timeProvider;
		this.keyRenderer = keyRenderer;
		this.rowKeyStrategy = rowKeyStrategy;
	}

	public ChannelConfiguration createChannel(String name, Long ttl) {
		ChannelConfiguration channelConfig = new ChannelConfiguration(name, timeProvider.getDate(), ttl);
		createColumnFamilyForChannel(channelConfig);
		insertChannelMetadata(channelConfig);
		return channelConfig;
	}

	public int countChannels() {
		QueryResult<Integer> result = hector.createCountQuery(connector.getKeyspace(), StringSerializer.get(), StringSerializer.get())
											.setKey(CHANNELS_ROW_KEY)
											.setColumnFamily(CHANNELS_COLUMN_FAMILY_NAME)
											.setRange(null, null, Integer.MAX_VALUE)
											.execute();
		return result.get();
	}

	private void insertChannelMetadata(ChannelConfiguration channelConfig) {
		StringSerializer keySerializer = StringSerializer.get();
		Mutator<String> mutator = connector.buildMutator(keySerializer);
		HColumn<String, ChannelConfiguration> column = hector.createColumn(channelConfig.getName(), channelConfig, StringSerializer.get(),
				channelConfigSerializer);
		mutator.insert(CHANNELS_ROW_KEY, CHANNELS_COLUMN_FAMILY_NAME, column);
	}

	public void initializeMetadata() {
		logger.info("Initializing channel metadata column family " + CHANNELS_COLUMN_FAMILY_NAME);
		connector.createColumnFamily(CHANNELS_COLUMN_FAMILY_NAME, false);
	}

	private void createColumnFamilyForChannel(ChannelConfiguration channelConfig) {
		String columnSpaceName = channelConfig.getName();
		connector.createColumnFamily(columnSpaceName);
	}

	public boolean channelExists(String channelName) {
		ChannelConfiguration channelConfiguration = getChannelConfiguration(channelName);
		return channelConfiguration != null;
	}

	public ChannelConfiguration getChannelConfiguration(String channelName) {
		Keyspace keyspace = connector.getKeyspace();
		ColumnQuery<String, String, ChannelConfiguration> rawQuery = hector.createColumnQuery(keyspace, StringSerializer.get(),
				StringSerializer.get(), channelConfigSerializer);
		ColumnQuery<String, String, ChannelConfiguration> columnQuery = rawQuery.setName(channelName)
																				.setKey(CHANNELS_ROW_KEY)
																				.setColumnFamily(CHANNELS_COLUMN_FAMILY_NAME);
		QueryResult<HColumn<String, ChannelConfiguration>> result = columnQuery.execute();
		HColumn<String, ChannelConfiguration> column = result.get();
		return column == null ? null : column.getValue();
	}

	public Iterable<ChannelConfiguration> getChannels() {
		Keyspace keyspace = connector.getKeyspace();
		SliceQuery<String, String, ChannelConfiguration> sliceQuery = hector.createSliceQuery(keyspace, StringSerializer.get(),
				StringSerializer.get(),
				channelConfigSerializer);
		SliceQuery<String, String, ChannelConfiguration> query = sliceQuery.setKey(CHANNELS_ROW_KEY).setColumnFamily(CHANNELS_COLUMN_FAMILY_NAME);

		ColumnSliceIterator<String, String, ChannelConfiguration> iterator = hector.createColumnSliceIterator(query, null, MAX_CHANNEL_NAME, false);
		List<ChannelConfiguration> result = new ArrayList<>();
		while (iterator.hasNext()) {
			HColumn<String, ChannelConfiguration> column = iterator.next();
			ChannelConfiguration config = column.getValue();
			result.add(config);
		}
		return result;
	}

	public void updateFirstKey(String channelName, DataHubKey key ) {
		updateMetadataKey(channelName, key, CHANNELS_FIRST_ROW_KEY);
	}

	public void deleteFirstKey(String channelName, DataHubKey key ) {
		deleteMetadataKey(channelName, key, CHANNELS_FIRST_ROW_KEY);
	}

	public void updateLastUpdatedKey(String channelName, DataHubKey key) {
		updateMetadataKey(channelName, key, CHANNELS_LATEST_ROW_KEY);
	}

	public void deleteLastUpdatedKey(String channelName, DataHubKey key) {
		deleteMetadataKey(channelName, key, CHANNELS_LATEST_ROW_KEY);
	}

	private void deleteMetadataKey(String channelName, DataHubKey hubKey, String rowKey ) {
		StringSerializer keySerializer = StringSerializer.get();
		Mutator<String> mutator = connector.buildMutator(keySerializer);
		mutator.delete(rowKey, channelName, channelName, keySerializer);
	}

	private void updateMetadataKey(String channelName, DataHubKey hubKey, String rowKey ) {
		StringSerializer keySerializer = StringSerializer.get();
		Mutator<String> mutator = connector.buildMutator(keySerializer);
		String keyString = keyRenderer.keyToString(hubKey);
		HColumn<String, String> column = hector.createColumn(channelName, keyString, StringSerializer.get(), StringSerializer.get());
		mutator.insert(rowKey, channelName, column);
	}

	public DataHubKey getFirstKey(String channelName) {
		return getMetadataKey(channelName, CHANNELS_FIRST_ROW_KEY);
	}

	public DataHubKey getLastUpdatedKey(String channelName) {
		return getMetadataKey(channelName, CHANNELS_LATEST_ROW_KEY);
	}

	private DataHubKey getMetadataKey(String channelName, String key) {
		Keyspace keyspace = connector.getKeyspace();
		ColumnQuery<String, String, String> rawQuery = hector.createColumnQuery(
				keyspace, StringSerializer.get(), StringSerializer.get(), StringSerializer.get());
		ColumnQuery<String, String, String> columnQuery = rawQuery
				.setName(channelName)
				.setKey(key)
				.setColumnFamily(channelName);
		QueryResult<HColumn<String, String>> result = columnQuery.execute();
		HColumn<String, String> column = result.get();
		return column == null ? null : keyRenderer.fromString(column.getValue());
	}

	public Collection<DataHubKey> findKeysInRange(String channelName, Date startTime, Date endTime) {
		DataHubKey minKey = new DataHubKey(startTime, (short) 0);
		DataHubKey maxKey = new DataHubKey(endTime, Short.MAX_VALUE);
		String minColumnKey = keyRenderer.keyToString(minKey);
		String maxColumnKey = keyRenderer.keyToString(maxKey);
		Keyspace keyspace = connector.getKeyspace();
		String minRowKey = rowKeyStrategy.buildKey(channelName, minKey);
		String maxRowKey = rowKeyStrategy.buildKey(channelName, maxKey);
		QueryResult<OrderedRows<String,String,DataHubCompositeValue>> results =
			hector.createRangeSlicesQuery(keyspace, StringSerializer.get(), StringSerializer.get(), DataHubCompositeValueSerializer.get())
				.setColumnFamily(channelName)
				.setRange(minColumnKey, maxColumnKey, false, Integer.MAX_VALUE)
				.setKeys(minRowKey, maxRowKey)
				.execute();
		Collection<DataHubKey> keys = new ArrayList<>();
		for (Row<String, String, DataHubCompositeValue> row : results.get().getList()) {
			for (HColumn<String, DataHubCompositeValue> column : row.getColumnSlice().getColumns()) {
				keys.add( keyRenderer.fromString( column.getName() ) );
			}
		}
		return keys;
	}
}

package com.flightstats.datahub.dao;

import com.flightstats.datahub.model.ChannelConfiguration;
import com.flightstats.datahub.model.DataHubKey;
import com.flightstats.datahub.util.DataHubKeyRenderer;
import com.flightstats.datahub.util.TimeProvider;
import com.google.inject.Inject;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.Serializer;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.mutation.Mutator;
import me.prettyprint.hector.api.query.ColumnQuery;
import me.prettyprint.hector.api.query.QueryResult;

/**
 * Encapsulates the channel creation, existence checks, and associated metadata.
 */
public class CassandraChannelsCollection {

	static final String CHANNELS_ROW_KEY = "DATA_HUB_CHANNELS";
	static final String CHANNELS_LATEST_ROW_KEY = "DATA_HUB_CHANNELS_LATEST";
	static final String CHANNELS_COLUMN_FAMILY_NAME = "channelMetadata";

	private final CassandraConnector connector;
	private final Serializer<ChannelConfiguration> channelConfigSerializer;
	private final HectorFactoryWrapper hector;
	private final TimeProvider timeProvider;
	private final DataHubKeyRenderer keyRenderer;

	@Inject
	public CassandraChannelsCollection(CassandraConnector connector, Serializer<ChannelConfiguration> channelConfigSerializer, HectorFactoryWrapper hector, TimeProvider timeProvider, DataHubKeyRenderer keyRenderer) {
		this.connector = connector;
		this.channelConfigSerializer = channelConfigSerializer;
		this.hector = hector;
		this.timeProvider = timeProvider;
		this.keyRenderer = keyRenderer;
	}

	public ChannelConfiguration createChannel(String name) {
		ChannelConfiguration channelConfig = new ChannelConfiguration(name, timeProvider.getDate());
		createColumnFamilyForChannel(channelConfig);
		//		createColumnFamilyForLastUpdateKey(channelConfig);
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

	public void updateLastUpdatedKey(String channelName, DataHubKey key) {
		StringSerializer keySerializer = StringSerializer.get();
		Mutator<String> mutator = connector.buildMutator(keySerializer);
		String keyString = keyRenderer.keyToString(key);
		HColumn<String, String> column = hector.createColumn(channelName, keyString, StringSerializer.get(), StringSerializer.get());
		mutator.insert(CHANNELS_LATEST_ROW_KEY, channelName, column);
	}

	public DataHubKey getLastUpdatedKey(String channelName) {
		Keyspace keyspace = connector.getKeyspace();
		ColumnQuery<String, String, String> rawQuery = hector.createColumnQuery(keyspace, StringSerializer.get(), StringSerializer.get(),
				StringSerializer.get());
		ColumnQuery<String, String, String> columnQuery = rawQuery.setName(channelName)
																  .setKey(CHANNELS_LATEST_ROW_KEY)
																  .setColumnFamily(channelName);
		QueryResult<HColumn<String, String>> result = columnQuery.execute();
		HColumn<String, String> column = result.get();
		return column == null ? null : keyRenderer.fromString(column.getValue());
	}
}

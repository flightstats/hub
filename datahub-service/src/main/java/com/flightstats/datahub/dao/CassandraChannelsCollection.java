package com.flightstats.datahub.dao;

import com.flightstats.datahub.model.ChannelConfiguration;
import com.flightstats.datahub.model.DataHubKey;
import com.flightstats.datahub.util.DataHubKeyRenderer;
import com.flightstats.datahub.util.TimeProvider;
import com.google.common.base.Strings;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.cassandra.service.ColumnSliceIterator;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.Serializer;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.mutation.Mutator;
import me.prettyprint.hector.api.query.ColumnQuery;
import me.prettyprint.hector.api.query.QueryResult;
import me.prettyprint.hector.api.query.SliceQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

/**
 * Encapsulates the channel creation, existence checks, and associated metadata.
 */
public class CassandraChannelsCollection {

	private final static Logger logger = LoggerFactory.getLogger(CassandraChannelsCollection.class);
    public static final String DATA_HUB_COLUMN_FAMILY_NAME = "DataHub";
	static final String CHANNELS_ROW_KEY = "DATA_HUB_CHANNELS";
	static final String CHANNELS_FIRST_ROW_KEY = "DATA_HUB_CHANNELS_FIRST";
	static final String CHANNELS_METADATA_COLUMN_FAMILY_NAME = "channelMetadata";
	static final String MAX_CHANNEL_NAME = Strings.repeat("~", 255);

	private final CassandraConnector connector;
	private final Serializer<ChannelConfiguration> channelConfigSerializer;
	private final HectorFactoryWrapper hector;
	private final TimeProvider timeProvider;
	private final DataHubKeyRenderer keyRenderer;
    private final ConcurrentMap<String,ChannelConfiguration> channelConfigurationMap;

	@Inject
	public CassandraChannelsCollection(CassandraConnector connector,
                                       Serializer<ChannelConfiguration> channelConfigSerializer,
                                       HectorFactoryWrapper hector, TimeProvider timeProvider,
                                       DataHubKeyRenderer keyRenderer,
                                       @Named("ChannelConfigurationMap") ConcurrentMap<String, ChannelConfiguration> channelConfigurationMap) {
		this.connector = connector;
		this.channelConfigSerializer = channelConfigSerializer;
		this.hector = hector;
		this.timeProvider = timeProvider;
		this.keyRenderer = keyRenderer;
        this.channelConfigurationMap = channelConfigurationMap;
	}

	public ChannelConfiguration createChannel(String name, Long ttlMillis) {
		ChannelConfiguration channelConfig = new ChannelConfiguration(name, timeProvider.getDate(), ttlMillis);
		insertChannelMetadata(channelConfig);
		return channelConfig;
	}

	public void updateChannel(ChannelConfiguration newConfig) {
		insertChannelMetadata(newConfig);
	}

	public int countChannels() {
		QueryResult<Integer> result = hector.createCountQuery(connector.getKeyspace(), StringSerializer.get(), StringSerializer.get())
											.setKey(CHANNELS_ROW_KEY)
											.setColumnFamily(CHANNELS_METADATA_COLUMN_FAMILY_NAME)
											.setRange(null, null, Integer.MAX_VALUE)
											.execute();
		return result.get();
	}

	private void insertChannelMetadata(ChannelConfiguration channelConfig) {
		StringSerializer keySerializer = StringSerializer.get();
		Mutator<String> mutator = connector.buildMutator(keySerializer);
		HColumn<String, ChannelConfiguration> column = hector.createColumn(channelConfig.getName(), channelConfig, StringSerializer.get(),
				channelConfigSerializer);
		mutator.insert(CHANNELS_ROW_KEY, CHANNELS_METADATA_COLUMN_FAMILY_NAME, column);
        channelConfigurationMap.put(channelConfig.getName(), channelConfig);
	}

	public void initializeMetadata() {
		logger.info("Initializing channel metadata column family " + CHANNELS_METADATA_COLUMN_FAMILY_NAME);
		connector.createColumnFamily(CHANNELS_METADATA_COLUMN_FAMILY_NAME, false);
	}

	public boolean channelExists(String channelName) {
		ChannelConfiguration channelConfiguration = getChannelConfiguration(channelName);
		return channelConfiguration != null;
	}

	public ChannelConfiguration getChannelConfiguration(String channelName) {
        if (channelConfigurationMap.containsKey(channelName)) {
            return channelConfigurationMap.get(channelName);
        }
		Keyspace keyspace = connector.getKeyspace();
		ColumnQuery<String, String, ChannelConfiguration> rawQuery = hector.createColumnQuery(keyspace, StringSerializer.get(),
		                                                                                      StringSerializer.get(), channelConfigSerializer);
		ColumnQuery<String, String, ChannelConfiguration> columnQuery = rawQuery.setName(channelName)
																				.setKey(CHANNELS_ROW_KEY)
																				.setColumnFamily(CHANNELS_METADATA_COLUMN_FAMILY_NAME);
		QueryResult<HColumn<String, ChannelConfiguration>> result = columnQuery.execute();
		HColumn<String, ChannelConfiguration> column = result.get();
        if (column == null) {
            return null;
        } else {
            ChannelConfiguration configuration = column.getValue();
            channelConfigurationMap.put(channelName, configuration);
            return configuration;
        }
	}

	public Iterable<ChannelConfiguration> getChannels() {
		Keyspace keyspace = connector.getKeyspace();
		SliceQuery<String, String, ChannelConfiguration> sliceQuery = hector.createSliceQuery(keyspace, StringSerializer.get(),
			StringSerializer.get(),
			channelConfigSerializer);
		SliceQuery<String, String, ChannelConfiguration> query = sliceQuery.setKey(CHANNELS_ROW_KEY).setColumnFamily(
			CHANNELS_METADATA_COLUMN_FAMILY_NAME);

		ColumnSliceIterator<String, String, ChannelConfiguration> iterator = hector.createColumnSliceIterator(query, null, MAX_CHANNEL_NAME, false);
		List<ChannelConfiguration> result = new ArrayList<>();
		while (iterator.hasNext()) {
			HColumn<String, ChannelConfiguration> column = iterator.next();
			ChannelConfiguration config = column.getValue();
			result.add(config);
		}
		return result;
	}

	public void updateFirstKey(String channelName, DataHubKey key) {
		updateMetadataKey(channelName, key, channelName + ":" + CHANNELS_FIRST_ROW_KEY);
	}

	public void deleteFirstKey(String channelName) {
		deleteMetadataKey(channelName, channelName + ":" + CHANNELS_FIRST_ROW_KEY);
	}

	private void deleteMetadataKey(String channelName, String rowKey) {
		StringSerializer keySerializer = StringSerializer.get();
		Mutator<String> mutator = connector.buildMutator(keySerializer);
		mutator.delete(rowKey, DATA_HUB_COLUMN_FAMILY_NAME, channelName, keySerializer);
	}

	private void updateMetadataKey(String channelName, DataHubKey hubKey, String rowKey) {
		StringSerializer keySerializer = StringSerializer.get();
		Mutator<String> mutator = connector.buildMutator(keySerializer);
		String keyString = keyRenderer.keyToString(hubKey);
		HColumn<String, String> column = hector.createColumn(channelName, keyString, StringSerializer.get(), StringSerializer.get());
		mutator.insert(rowKey, DATA_HUB_COLUMN_FAMILY_NAME, column);
	}

	public DataHubKey getFirstKey(String channelName) {
		return getMetadataKey(channelName, channelName + ":" + CHANNELS_FIRST_ROW_KEY);
	}

	private DataHubKey getMetadataKey(String channelName, String key) {
		Keyspace keyspace = connector.getKeyspace();
		ColumnQuery<String, String, String> rawQuery = hector.createColumnQuery(
				keyspace, StringSerializer.get(), StringSerializer.get(), StringSerializer.get());
		ColumnQuery<String, String, String> columnQuery = rawQuery
				.setName(channelName)
				.setKey(key)
				.setColumnFamily(DATA_HUB_COLUMN_FAMILY_NAME);
		QueryResult<HColumn<String, String>> result = columnQuery.execute();
		HColumn<String, String> column = result.get();
		return column == null ? null : keyRenderer.fromString(column.getValue()).get();
	}

	public boolean isChannelMetadataRowKey(String key) {
		return Strings.nullToEmpty(key).equals(CHANNELS_FIRST_ROW_KEY);
	}
}

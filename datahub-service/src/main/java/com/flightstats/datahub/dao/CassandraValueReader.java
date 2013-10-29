package com.flightstats.datahub.dao;

import com.flightstats.datahub.dao.serialize.DataHubCompositeValueSerializer;
import com.flightstats.datahub.model.DataHubCompositeValue;
import com.flightstats.datahub.model.DataHubKey;
import com.flightstats.datahub.util.DataHubKeyRenderer;
import com.google.inject.Inject;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.exceptions.HInvalidRequestException;
import me.prettyprint.hector.api.query.ColumnQuery;
import me.prettyprint.hector.api.query.QueryResult;

import static com.flightstats.datahub.dao.CassandraChannelsCollection.DATA_HUB_COLUMN_FAMILY_NAME;
import static com.flightstats.datahub.dao.CassandraUtils.maybePromoteToNoSuchChannel;

public class CassandraValueReader {

	private final CassandraConnector connector;
	private final HectorFactoryWrapper hector;
	private final RowKeyStrategy<String, DataHubKey, DataHubCompositeValue> rowKeyStrategy;
	private final DataHubKeyRenderer keyRenderer;

	@Inject
	public CassandraValueReader(CassandraConnector connector, HectorFactoryWrapper hector, RowKeyStrategy<String, DataHubKey,
			DataHubCompositeValue> rowKeyStrategy, DataHubKeyRenderer keyRenderer) {
		this.connector = connector;
		this.hector = hector;
		this.rowKeyStrategy = rowKeyStrategy;
		this.keyRenderer = keyRenderer;
	}

	public DataHubCompositeValue read(String channelName, DataHubKey key) {
		Keyspace keyspace = connector.getKeyspace();
		ColumnQuery<String, String, DataHubCompositeValue> query = hector.createColumnQuery(keyspace, StringSerializer.get(), StringSerializer.get(),
				DataHubCompositeValueSerializer.get());
		String rowKey = rowKeyStrategy.buildKey(channelName, key);
		try {
			return executeQuery(key, query, rowKey);

		} catch (HInvalidRequestException e) {
			throw maybePromoteToNoSuchChannel(e, channelName);
		}
	}

	private DataHubCompositeValue executeQuery(DataHubKey key, ColumnQuery<String, String, DataHubCompositeValue> query, String rowKey) {
		QueryResult<HColumn<String, DataHubCompositeValue>> queryResult = query.setColumnFamily(DATA_HUB_COLUMN_FAMILY_NAME)
																			   .setKey(rowKey)
																			   .setName(keyRenderer.keyToString(key))
																			   .execute();
		HColumn<String, DataHubCompositeValue> column = queryResult.get();
		return column == null ? null : column.getValue();
	}

}

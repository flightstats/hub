package com.flightstats.datahub.dao;

import com.flightstats.datahub.dao.serialize.DataHubCompositeValueSerializer;
import com.flightstats.datahub.model.DataHubCompositeValue;
import com.flightstats.datahub.model.DataHubKey;
import com.flightstats.datahub.model.ValueInsertionResult;
import com.flightstats.datahub.util.DataHubKeyGenerator;
import com.flightstats.datahub.util.DataHubKeyRenderer;
import com.google.inject.Inject;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.exceptions.HInvalidRequestException;
import me.prettyprint.hector.api.mutation.Mutator;

import java.util.Collection;

import static com.flightstats.datahub.dao.CassandraChannelsCollection.DATA_HUB_COLUMN_FAMILY_NAME;
import static com.flightstats.datahub.dao.CassandraUtils.maybePromoteToNoSuchChannel;

public class CassandraValueWriter {

	private final CassandraConnector connector;
	private final HectorFactoryWrapper hector;
	private final RowKeyStrategy<String, DataHubKey, DataHubCompositeValue> rowKeyStrategy;
	private final DataHubKeyGenerator keyGenerator;
	private final DataHubKeyRenderer keyRenderer;

	@Inject
	public CassandraValueWriter(CassandraConnector connector, HectorFactoryWrapper hector, RowKeyStrategy<String, DataHubKey, DataHubCompositeValue> rowKeyStrategy, DataHubKeyGenerator keyGenerator, DataHubKeyRenderer keyRenderer) {
		this.connector = connector;
		this.hector = hector;
		this.rowKeyStrategy = rowKeyStrategy;
		this.keyGenerator = keyGenerator;
		this.keyRenderer = keyRenderer;
	}

	public ValueInsertionResult write(String channelName, DataHubCompositeValue columnValue, int ttlSeconds) {
		Mutator<String> mutator = connector.buildMutator(StringSerializer.get());

		DataHubKey key = keyGenerator.newKey(channelName);

		String columnName = keyRenderer.keyToString(key);
		HColumn<String, DataHubCompositeValue> column = hector.createColumn(columnName, columnValue, ttlSeconds, StringSerializer.get(),
				DataHubCompositeValueSerializer.get());

		String rowKey = rowKeyStrategy.buildKey(channelName, key);

		try {
			mutator.insert(rowKey, DATA_HUB_COLUMN_FAMILY_NAME, column);
		} catch (HInvalidRequestException e) {
			throw maybePromoteToNoSuchChannel(e, channelName);
		}
		return new ValueInsertionResult(key, rowKey);
	}

	public void delete(String channelName, Collection<DataHubKey> keys) {
		if ( keys.isEmpty() ) return;

		Mutator<String> mutator = connector.buildMutator(StringSerializer.get());
		for (DataHubKey key : keys) {
			String rowKey = rowKeyStrategy.buildKey(channelName, key);
			String columnName = keyRenderer.keyToString(key);
			mutator.addDeletion(rowKey, DATA_HUB_COLUMN_FAMILY_NAME, columnName, StringSerializer.get());
		}
		mutator.execute();
	}
}

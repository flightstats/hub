package com.flightstats.datahub.dao;

import com.flightstats.datahub.model.ChannelConfiguration;
import com.flightstats.datahub.model.DataHubCompositeValue;
import com.flightstats.datahub.model.DataHubKey;
import com.flightstats.datahub.util.DataHubKeyRenderer;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.query.ColumnQuery;
import me.prettyprint.hector.api.query.QueryResult;

public class CassandraValueReader {

    private final CassandraConnector connector;
    private final HectorFactoryWrapper hector;
    private final RowKeyStrategy<String, DataHubKey, DataHubCompositeValue> rowKeyStrategy;
    private final CassandraChannelsCollection channelsCollection;
    private final DataHubKeyRenderer keyRenderer;

    @Inject
    public CassandraValueReader(CassandraConnector connector, HectorFactoryWrapper hector, RowKeyStrategy<String, DataHubKey,
            DataHubCompositeValue> rowKeyStrategy, CassandraChannelsCollection channelsCollection, DataHubKeyRenderer keyRenderer) {
        this.connector = connector;
        this.hector = hector;
        this.rowKeyStrategy = rowKeyStrategy;
        this.channelsCollection = channelsCollection;
        this.keyRenderer = keyRenderer;
    }

    public DataHubCompositeValue read(String channelName, DataHubKey key) {
        Keyspace keyspace = connector.getKeyspace();
        ColumnQuery<String, String, DataHubCompositeValue> query = hector.createColumnQuery(keyspace, StringSerializer.get(), StringSerializer.get(),
                DataHubCompositeValueSerializer.get());
        String rowKey = rowKeyStrategy.buildKey(channelName, key);
        QueryResult<HColumn<String, DataHubCompositeValue>> queryResult = query.setColumnFamily(channelName)
                                                                               .setKey(rowKey)
                                                                               .setName(keyRenderer.keyToString(key))
                                                                               .execute();
        HColumn<String, DataHubCompositeValue> column = queryResult.get();
        return column == null ? null : column.getValue();
    }

    public Optional<DataHubKey> findLatestId(String channelName) {
        ChannelConfiguration config = channelsCollection.getChannelConfiguration(channelName);
		if ( config == null )
		{
			return Optional.absent();
		}
        DataHubKey lastUpdateKey = config.getLastUpdateKey();
        if (lastUpdateKey == null) {
            return Optional.absent();
        }
        return Optional.of(lastUpdateKey);
    }

}

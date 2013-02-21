package com.flightstats.datahub.dao;

import com.flightstats.datahub.model.ChannelConfiguration;
import com.flightstats.datahub.model.DataHubCompositeValue;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.cassandra.serializers.UUIDSerializer;
import me.prettyprint.cassandra.service.ColumnSliceIterator;
import me.prettyprint.cassandra.utils.TimeUUIDUtils;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.query.ColumnQuery;
import me.prettyprint.hector.api.query.QueryResult;
import me.prettyprint.hector.api.query.SliceQuery;

import java.util.Date;
import java.util.UUID;

public class CassandraValueReader {

    private final CassandraConnector connector;
    private final HectorFactoryWrapper hector;
    private final RowKeyStrategy<String, UUID, DataHubCompositeValue> rowKeyStrategy;
    private final CassandraChannelsCollection channelsCollection;

    @Inject
    public CassandraValueReader(CassandraConnector connector, HectorFactoryWrapper hector, RowKeyStrategy<String, UUID, DataHubCompositeValue> rowKeyStrategy, CassandraChannelsCollection channelsCollection) {
        this.connector = connector;
        this.hector = hector;
        this.rowKeyStrategy = rowKeyStrategy;
        this.channelsCollection = channelsCollection;
    }

    public DataHubCompositeValue read(String channelName, UUID id) {
        Keyspace keyspace = connector.getKeyspace();
        ColumnQuery<String, UUID, DataHubCompositeValue> query = hector.createColumnQuery(keyspace, StringSerializer.get(), UUIDSerializer.get(),
                DataHubCompositeValueSerializer.get());
        String key = rowKeyStrategy.buildKey(channelName, id);
        QueryResult<HColumn<UUID, DataHubCompositeValue>> queryResult = query.setColumnFamily(channelName)
                                                                             .setKey(key)
                                                                             .setName(id)
                                                                             .execute();
        HColumn<UUID, DataHubCompositeValue> column = queryResult.get();
        return column == null ? null : column.getValue();
    }

    public Optional<UUID> findLatestId(String channelName) {

        Keyspace keyspace = connector.getKeyspace();
        SliceQuery<String, UUID, DataHubCompositeValue> rawSliceQuery = hector.createSliceQuery(keyspace, StringSerializer.get(),
                UUIDSerializer.get(),
                DataHubCompositeValueSerializer.get());

        ChannelConfiguration config = channelsCollection.getChannelConfiguration(channelName);
        Date lastUpdateDate = config.getLastUpdateDate();
        if (lastUpdateDate == null) {
            return Optional.absent();
        }
        UUID uidForLastUpdateTime = TimeUUIDUtils.getTimeUUID(lastUpdateDate.getTime());
        String key = rowKeyStrategy.buildKey(channelName, uidForLastUpdateTime);

        UUID minUUID = new UUID(0x0000000000000000L, 0x0000000000000000L);
        UUID maxUUID = new UUID(0xFFFFFFFFFFFFFFFFL, 0xFFFFFFFFFFFFFFFFL);
        SliceQuery<String, UUID, DataHubCompositeValue> sliceQuery = rawSliceQuery.setColumnFamily(channelName)
                                                                                  .setKey(key);
        ColumnSliceIterator<String, UUID, DataHubCompositeValue> iterator = new ColumnSliceIterator<>(sliceQuery, maxUUID, minUUID, true, 1);

        if (!iterator.hasNext()) {
            return null;
        }
        HColumn<UUID, DataHubCompositeValue> column = iterator.next();
        return Optional.of(column.getName());
    }

}

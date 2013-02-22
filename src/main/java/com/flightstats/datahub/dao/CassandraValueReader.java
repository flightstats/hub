package com.flightstats.datahub.dao;

import com.flightstats.datahub.model.ChannelConfiguration;
import com.flightstats.datahub.model.DataHubCompositeValue;
import com.flightstats.datahub.model.DataHubKey;
import com.flightstats.datahub.util.DataHubKeyRenderer;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.cassandra.service.ColumnSliceIterator;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.query.ColumnQuery;
import me.prettyprint.hector.api.query.QueryResult;
import me.prettyprint.hector.api.query.SliceQuery;
import org.joda.time.DurationFieldType;
import org.joda.time.LocalDate;

import java.util.Date;

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
        Date lastUpdateDate = config.getLastUpdateDate();
        if (lastUpdateDate == null) {
            return Optional.absent();
        }

        LocalDate localDate = new LocalDate(lastUpdateDate.getTime());
        Date beginningOfDay = localDate.toDateTimeAtStartOfDay().toDate();      //TODO: This knowledge shouldn't be in here.
        Date endOfDay = localDate.withFieldAdded(DurationFieldType.days(), 1)
                                 .toDateTimeAtStartOfDay()
                                 .toDate();      //TODO: This knowledge shouldn't be in here.

        DataHubKey minKeyForDay = new DataHubKey(beginningOfDay, (short) 0);
        DataHubKey maxKeyForDay = new DataHubKey(endOfDay, (short) 0);

        String maxColumnValue = keyRenderer.keyToString(maxKeyForDay);
        String minColumnValue = keyRenderer.keyToString(minKeyForDay);

        Keyspace keyspace = connector.getKeyspace();
        SliceQuery<String, String, DataHubCompositeValue> rawSliceQuery = hector.createSliceQuery(keyspace, StringSerializer.get(),
                StringSerializer.get(),
                DataHubCompositeValueSerializer.get());
        String rowKey = rowKeyStrategy.buildKey(channelName, new DataHubKey(lastUpdateDate, (short) 0));
        SliceQuery<String, String, DataHubCompositeValue> sliceQuery = rawSliceQuery.setColumnFamily(channelName).setKey(rowKey);

        ColumnSliceIterator<String, String, DataHubCompositeValue> iterator = new ColumnSliceIterator<>(sliceQuery, maxColumnValue, minColumnValue,
                true, 1);

        if (!iterator.hasNext()) {
            return null;
        }
        HColumn<String, DataHubCompositeValue> column = iterator.next();
        String columnName = column.getName();

        return Optional.of(keyRenderer.fromString(columnName));
    }

}

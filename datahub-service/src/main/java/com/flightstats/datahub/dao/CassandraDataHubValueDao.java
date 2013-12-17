package com.flightstats.datahub.dao;

import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.exceptions.AlreadyExistsException;
import com.flightstats.datahub.model.DataHubCompositeValue;
import com.flightstats.datahub.model.DataHubKey;
import com.flightstats.datahub.model.ValueInsertionResult;
import com.flightstats.datahub.util.DataHubKeyGenerator;
import com.flightstats.datahub.util.TimeProvider;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Collection;

public class CassandraDataHubValueDao implements DataHubValueDao {

    private final static Logger logger = LoggerFactory.getLogger(CassandraDataHubValueDao.class);

	private final RowKeyStrategy<String, DataHubKey, DataHubCompositeValue> rowKeyStrategy;
	private final DataHubKeyGenerator keyGenerator;
    private final TimeProvider timeProvider;
    private final Session session;
    private int gcGraceSeconds;

    @Inject
	public CassandraDataHubValueDao(RowKeyStrategy<String, DataHubKey,
            DataHubCompositeValue> rowKeyStrategy,
                                    DataHubKeyGenerator keyGenerator,
                                    TimeProvider timeProvider,
                                    Session session,
                                    @Named("cassandra.gc_grace_seconds") int gcGraceSeconds) {
		this.rowKeyStrategy = rowKeyStrategy;
		this.keyGenerator = keyGenerator;
        this.timeProvider = timeProvider;
        this.session = session;
        this.gcGraceSeconds = gcGraceSeconds;
    }

	@Override
    public ValueInsertionResult write(String channelName, DataHubCompositeValue columnValue, Optional<Integer> ttlSeconds) {
        DataHubKey key = keyGenerator.newKey(channelName);
        String rowKey = rowKeyStrategy.buildKey(channelName, key);

        Integer ttl = 0;
        if (ttlSeconds.isPresent())
        {
            ttl = ttlSeconds.get();
        }
        PreparedStatement statement = session.prepare("INSERT INTO values" +
                " (rowkey, sequence, data, millis, contentType, contentLanguage)" +
                "VALUES (?, ?, ?, ?, ?, ?) USING TTL " + ttl);
        statement.setConsistencyLevel(ConsistencyLevel.QUORUM);
        session.execute(statement.bind(rowKey, key.getSequence(), ByteBuffer.wrap(columnValue.getData()), columnValue.getMillis(),
                columnValue.getContentType().orNull(), columnValue.getContentLanguage().orNull()));
		return new ValueInsertionResult(key, rowKey, timeProvider.getDate());
	}

    @Override
    public void delete(String channelName, Collection<DataHubKey> keys) {
		//todo - gfm - 11/22/13 -
	}

    @Override
    public DataHubCompositeValue read(String channelName, DataHubKey key) {

        String rowKey = rowKeyStrategy.buildKey(channelName, key);
        PreparedStatement statement = session.prepare("SELECT * FROM values WHERE rowkey = ? and sequence = ?");
        statement.setConsistencyLevel(ConsistencyLevel.QUORUM);
        Row row = session.execute(statement.bind(rowKey, key.getSequence())).one();
        //todo - gfm - 11/22/13 - test null
        if (row == null) {
            return null;
        }
        String contentType = row.getString("contentType");
        String contentLanguage = row.getString("contentLanguage");
        ByteBuffer data = row.getBytes("data");

        byte[] array = new byte[data.remaining()];
        data.get(array);

        return new DataHubCompositeValue(Optional.fromNullable(contentType),
                Optional.fromNullable(contentLanguage), array, row.getLong("millis"));
    }

    @Override
    public void initializeTable() {
        //todo - gfm - 11/19/13 - make more tables eventually?
        try {
            session.execute(
                    "CREATE TABLE values (" +
                            "rowkey text," +
                            "sequence bigint," +
                            "data blob," +
                            "millis bigint," +
                            "contentType text," +
                            "contentLanguage text," +
                            "PRIMARY KEY (rowkey, sequence)" +
                            ")");
            logger.info("created values table");
        } catch (AlreadyExistsException e) {
            logger.info("values table already exists");
        }
        session.execute("ALTER TABLE values with gc_grace_seconds = " + gcGraceSeconds);
    }
}

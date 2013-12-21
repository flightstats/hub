package com.flightstats.datahub.dao.cassandra;

import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.exceptions.AlreadyExistsException;
import com.flightstats.datahub.dao.DataHubValueDao;
import com.flightstats.datahub.model.*;
import com.flightstats.datahub.util.DataHubKeyGenerator;
import com.flightstats.datahub.util.TimeProvider;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

public class CassandraDataHubValueDao implements DataHubValueDao {

    private final static Logger logger = LoggerFactory.getLogger(CassandraDataHubValueDao.class);

	private final DataHubKeyGenerator keyGenerator;
    private final TimeProvider timeProvider;
    private final QuorumSession session;
    private int gcGraceSeconds;

    @Inject
	public CassandraDataHubValueDao(DataHubKeyGenerator keyGenerator,
                                    TimeProvider timeProvider,
                                    QuorumSession session,
                                    @Named("cassandra.gc_grace_seconds") int gcGraceSeconds) {
		this.keyGenerator = keyGenerator;
        this.timeProvider = timeProvider;
        this.session = session;
        this.gcGraceSeconds = gcGraceSeconds;
    }

	@Override
    public ValueInsertionResult write(String channelName, DataHubCompositeValue columnValue, Optional<Integer> ttlSeconds) {
        SequenceDataHubKey key = keyGenerator.newKey(channelName);
        String rowKey = buildKey(channelName, key);

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
    public DataHubCompositeValue read(String channelName, DataHubKey key) {

        SequenceDataHubKey sequenceKey = (SequenceDataHubKey) key;
        String rowKey = buildKey(channelName, sequenceKey);
        PreparedStatement statement = session.prepare("SELECT * FROM values WHERE rowkey = ? and sequence = ?");
        statement.setConsistencyLevel(ConsistencyLevel.QUORUM);
        Row row = session.execute(statement.bind(rowKey, sequenceKey.getSequence())).one();
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
    public void initialize() {
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

    @Override
    public void initializeChannel(ChannelConfiguration configuration) {
        keyGenerator.seedChannel(configuration.getName());
    }

    @Override
    public Optional<DataHubKey> getKey(String id) {
        return SequenceDataHubKey.fromString(id);
    }

    private static final long INCREMENT = 1000;

    private String buildKey(String channelName, SequenceDataHubKey dataHubKey) {
        return channelName + ":" + (dataHubKey.getSequence() / INCREMENT);
    }

}

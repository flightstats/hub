package com.flightstats.datahub.dao.cassandra;

import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.exceptions.AlreadyExistsException;
import com.flightstats.datahub.dao.ChannelsCollectionDao;
import com.flightstats.datahub.model.ChannelConfiguration;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Encapsulates the channel creation, existence checks, and associated metadata.
 */
public class CassandraChannelsCollectionDao implements ChannelsCollectionDao {

	private final static Logger logger = LoggerFactory.getLogger(CassandraChannelsCollectionDao.class);

    private QuorumSession session;

    @Inject
	public CassandraChannelsCollectionDao(QuorumSession session) {
        this.session = session;
    }

	@Override
    public ChannelConfiguration createChannel(ChannelConfiguration configuration) {
		insertChannelMetadata(configuration);
		return configuration;
	}

	@Override
    public void updateChannel(ChannelConfiguration newConfig) {
		insertChannelMetadata(newConfig);
	}

	@Override
    public void initializeMetadata() {
		logger.info("Initializing channel metadata table");
        try {
            session.execute(
                    "CREATE TABLE channelMetadata (" +
                            "name text PRIMARY KEY," +
                            "creationDate timestamp," +
                            "ttlMillis bigint" +
                            ");");
            logger.info("created channel metadata table");
        } catch (AlreadyExistsException e) {
            logger.info( "channelMetadata table already exists");
        }
	}

    private void insertChannelMetadata(ChannelConfiguration channelConfig) {

        PreparedStatement statement = session.prepare("INSERT INTO channelMetadata" +
                " (name, creationDate, ttlMillis)" +
                "VALUES (?, ?, ?)");

        session.execute(statement.bind(channelConfig.getName(), channelConfig.getCreationDate(), channelConfig.getTtlMillis()));
    }

	@Override
    public boolean channelExists(String channelName) {
        return getChannelConfiguration(channelName) != null;
	}

	@Override
    public ChannelConfiguration getChannelConfiguration(String channelName) {
        PreparedStatement statement = session.prepare("SELECT * FROM channelMetadata WHERE name = ? ");
        Row row = session.execute(statement.bind(channelName)).one();
        if (row == null) {
            return null;
        }
        return createChannelConfig(row);
    }

	@Override
    public Iterable<ChannelConfiguration> getChannels() {
        List<ChannelConfiguration> result = new ArrayList<>();
        ResultSet results = session.execute("SELECT * FROM channelMetadata");
        for (Row row : results) {
            result.add(createChannelConfig(row));
        }
		return result;
	}

    @Override
    public boolean isHealthy() {
        try {
            getChannels();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private ChannelConfiguration createChannelConfig(Row row) {
        return ChannelConfiguration.builder()
                .withName(row.getString("name"))
                .withTtlMillis(row.getLong("ttlMillis"))
                .withCreationDate(row.getDate("creationDate"))
                .build();
    }


}

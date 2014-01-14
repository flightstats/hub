package com.flightstats.datahub.dao.cassandra;

import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.exceptions.AlreadyExistsException;
import com.flightstats.datahub.dao.ChannelMetadataDao;
import com.flightstats.datahub.model.ChannelConfiguration;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Encapsulates the channel creation, existence checks, and associated metadata.
 */
public class CassandraChannelMetadataDao implements ChannelMetadataDao {

	private final static Logger logger = LoggerFactory.getLogger(CassandraChannelMetadataDao.class);

    private QuorumSession session;

    @Inject
	public CassandraChannelMetadataDao(QuorumSession session) {
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
                            "ttlDays bigint" +
                            ");");
            logger.info("created channel metadata table");
        } catch (AlreadyExistsException e) {
            logger.info( "channelMetadata table already exists");
        }
	}

    private void insertChannelMetadata(ChannelConfiguration channelConfig) {

        PreparedStatement statement = session.prepare("INSERT INTO channelMetadata" +
                " (name, creationDate, ttlDays)" +
                "VALUES (?, ?, ?)");

        session.execute(statement.bind(channelConfig.getName(), channelConfig.getCreationDate(), channelConfig.getTtlDays()));
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
    public void delete(String channelName) {
        logger.warn("Cassandra impl doesn't support deleting channels " + channelName);
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
                .withTtlDays(row.getLong("ttlDays"))
                .withCreationDate(row.getDate("creationDate"))
                .build();
    }


}

package com.flightstats.datahub.dao;

import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.exceptions.AlreadyExistsException;
import com.flightstats.datahub.model.ChannelConfiguration;
import com.flightstats.datahub.util.TimeProvider;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

/**
 * Encapsulates the channel creation, existence checks, and associated metadata.
 */
public class CassandraChannelsCollection {

	private final static Logger logger = LoggerFactory.getLogger(CassandraChannelsCollection.class);

    private final TimeProvider timeProvider;
    private final ConcurrentMap<String,ChannelConfiguration> channelConfigurationMap;
    private Session session;

    @Inject
	public CassandraChannelsCollection(TimeProvider timeProvider,
                                       @Named("ChannelConfigurationMap") ConcurrentMap<String,
                                       ChannelConfiguration> channelConfigurationMap,
                                       Session session) {
		this.timeProvider = timeProvider;
        this.channelConfigurationMap = channelConfigurationMap;
        this.session = session;
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

        Row row = session.execute("SELECT count(*) FROM channelMetadata").one();
		return (int) row.getLong(0);
	}

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
        channelConfigurationMap.put(channelConfig.getName(), channelConfig);
    }

	public boolean channelExists(String channelName) {
		ChannelConfiguration channelConfiguration = getChannelConfiguration(channelName);
		return channelConfiguration != null;
	}

	public ChannelConfiguration getChannelConfiguration(String channelName) {
        //todo - gfm - 11/22/13 - pull out caching into separate class?
        ChannelConfiguration configuration = channelConfigurationMap.get(channelName);
        if (configuration != null) {
            return configuration;
        }
        PreparedStatement statement = session.prepare("SELECT * FROM channelMetadata WHERE name = ? ");
        Row row = session.execute(statement.bind(channelName)).one();
        if (row == null) {
            return null;
        }

        configuration = createChannelConfig(row);
        channelConfigurationMap.put(channelName, configuration);
        return configuration;
    }

	public Iterable<ChannelConfiguration> getChannels() {
        List<ChannelConfiguration> result = new ArrayList<>();
        ResultSet results = session.execute("SELECT * FROM channelMetadata");
        Iterator<Row> iterator = results.iterator();
        while (iterator.hasNext()) {
            result.add(createChannelConfig(iterator.next()));
        }
		return result;
	}

    private ChannelConfiguration createChannelConfig(Row row) {
        return new ChannelConfiguration(row.getString("name"), row.getDate("creationDate"), row.getLong("ttlMillis"));
    }

    //todo - gfm - 11/22/13 - switch to cql if we still need this
    /*public void updateLatestRowKey(String channelName, String rowKeyValue) {
        Mutator<String> mutator = connector.buildMutator(StringSerializer.get());
        HColumn<String, String> column = hector.createColumn(channelName, rowKeyValue, StringSerializer.get(), StringSerializer.get());
        mutator.insert(latestKey(channelName), DATA_HUB_COLUMN_FAMILY_NAME, column);
    }

    private String latestKey(String channelName) {
        return channelName + ":" + CHANNELS_LATEST_ROW_KEY;
    }

    public String getLatestRowKey(String channelName) {
        Keyspace keyspace = connector.getKeyspace();
        ColumnQuery<String, String, String> rawQuery = hector.createColumnQuery(
                keyspace, StringSerializer.get(), StringSerializer.get(), StringSerializer.get());
        QueryResult<HColumn<String, String>> result = rawQuery
                .setKey(latestKey(channelName))
                .setName(channelName)
                .setColumnFamily(DATA_HUB_COLUMN_FAMILY_NAME).execute();
        HColumn<String, String> column = result.get();
        return column == null ? null : column.getValue();
    }*/

}

package com.flightstats.datahub.dao;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.exceptions.AlreadyExistsException;
import com.google.inject.Inject;
import com.google.inject.Provides;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import java.util.concurrent.TimeUnit;

import static com.google.common.util.concurrent.Uninterruptibles.sleepUninterruptibly;

public class CassandraConnectorFactory {

    private final static Logger logger = LoggerFactory.getLogger(CassandraConnectorFactory.class);
	private static final int SECONDS_BETWEEN_CONNECTION_RETRIES = 5;

    private final String host;
    private final Integer port;
    private final int replicationFactor;

    @Inject
	public CassandraConnectorFactory(@Named("cassandra.host") String host,
                                     @Named("cassandra.port") Integer port,
									 @Named("cassandra.replication_factor") int replicationFactor) {
        this.host = host;
        this.port = port;
        this.replicationFactor = replicationFactor;
    }

    @Provides
    public Session getSession() {
        while (true) {
            try {
                return attemptSession();
            } catch (Exception e) {
                //datastax driver doesn't use a common exception class to handle.
                logErrorAndWait(e);
            }
        }
    }

    private Session attemptSession() {
        String[] splitHosts = StringUtils.split(host, ",");
        com.datastax.driver.core.Cluster cluster = Cluster.builder()
                .addContactPoints(splitHosts).withPort(port)
                .build();
        addKeyspaceIfMissing(cluster.connect());
        return cluster.connect("datahub");
    }

    private void logErrorAndWait(Exception e) {
		logger.error("Error creating CassandraConnector: " + e.getMessage(), e);
		logger.info("Sleeping before retrying...");
		sleepUninterruptibly(SECONDS_BETWEEN_CONNECTION_RETRIES, TimeUnit.SECONDS);
		logger.info("Retrying cassandra connection");
	}

	private void addKeyspaceIfMissing(Session session) {
        //if we upgrade to Cassandra 2, can use CREATE KEYSPACE IF NOT EXISTS
        try {
            session.execute("CREATE KEYSPACE datahub" +
                    " WITH replication = {'class':'SimpleStrategy', 'replication_factor': " + replicationFactor + "};");
            logger.info("Created keyspace datahub");
        } catch (AlreadyExistsException e) {
            logger.info("keyspace alreayd exists");
        }
    }

}

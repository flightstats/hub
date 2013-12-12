package com.flightstats.datahub.dao.cassandra;

import com.datastax.driver.core.Cluster;
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
    public QuorumSession getSession() {
        while (true) {
            try {
                return attemptSession();
            } catch (Exception e) {
                //datastax driver doesn't use a common exception class to handle.
                logErrorAndWait(e);
            }
        }
    }

    private QuorumSession attemptSession() {
        String[] splitHosts = StringUtils.split(host, ",");
        com.datastax.driver.core.Cluster cluster = Cluster.builder()
                .addContactPoints(splitHosts).withPort(port)
                .build();
        addKeyspaceIfMissing(new QuorumSession(cluster.connect()));
        return new QuorumSession(cluster.connect("datahub"));
    }

    private void logErrorAndWait(Exception e) {
		logger.error("Error creating CassandraConnector: " + e.getMessage(), e);
		logger.info("Sleeping before retrying...");
		sleepUninterruptibly(SECONDS_BETWEEN_CONNECTION_RETRIES, TimeUnit.SECONDS);
		logger.info("Retrying cassandra connection");
	}

	private void addKeyspaceIfMissing(QuorumSession session) {
        String keyspaceCql = "KEYSPACE datahub WITH replication = {'class':'SimpleStrategy', 'replication_factor': " + replicationFactor + "};";
        try {
            session.execute("CREATE " + keyspaceCql);
            logger.info("Created keyspace datahub");
        } catch (AlreadyExistsException e) {
            logger.info("keyspace already exists");
        }
        session.execute("ALTER " + keyspaceCql);
    }

}

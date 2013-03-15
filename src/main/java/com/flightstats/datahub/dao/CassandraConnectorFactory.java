package com.flightstats.datahub.dao;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Provides;
import me.prettyprint.cassandra.service.CassandraHostConfigurator;
import me.prettyprint.cassandra.service.ThriftCluster;
import me.prettyprint.cassandra.service.ThriftKsDef;
import me.prettyprint.hector.api.Cluster;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.ddl.ColumnFamilyDefinition;
import me.prettyprint.hector.api.ddl.KeyspaceDefinition;
import me.prettyprint.hector.api.exceptions.HectorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static com.flightstats.datahub.dao.CassandraConnector.KEYSPACE_NAME;
import static com.google.common.util.concurrent.Uninterruptibles.sleepUninterruptibly;

public class CassandraConnectorFactory {

	private final static Logger logger = LoggerFactory.getLogger(CassandraConnectorFactory.class);
	private static final int TIME_BETWEEN_CONNECTION_RETRIES = 5;

	private final String clusterName;
	private final String hostPort;
	private final int replicationFactor;
	private final HectorFactoryWrapper hector;

	@Inject
	public CassandraConnectorFactory(@Named("cassandra.cluster_name") String clusterName, @Named("cassandra.hostport") String hostPort,
									 @Named("cassandra.replication_factor") int replicationFactor, HectorFactoryWrapper hector) {
		this.clusterName = clusterName;
		this.hostPort = hostPort;
		this.replicationFactor = replicationFactor;
		this.hector = hector;
	}

	@Provides
	public CassandraConnector build() {
		while (true) {
			try {
				return attemptConnection();
			} catch (HectorException e) {
				logErrorAndWait(e);
			}
		}
	}

	private CassandraConnector attemptConnection() {
		CassandraHostConfigurator hostConfigurator = new CassandraHostConfigurator(hostPort);
		Cluster cluster = getCluster(hostConfigurator);
		addKeyspaceIfMissing(replicationFactor, cluster);
		Keyspace keyspace = hector.createKeyspace(KEYSPACE_NAME, cluster);
		return new CassandraConnector(cluster, keyspace, hector);
	}

	private void logErrorAndWait(HectorException e) {
		logger.error("Error creating CassandraConnector: " + e.getMessage());
		logger.info("Sleeping before retrying...");
		sleepUninterruptibly(TIME_BETWEEN_CONNECTION_RETRIES, TimeUnit.SECONDS);
		logger.info("Retrying cassandra connection");
	}

	@VisibleForTesting
	Cluster getCluster(CassandraHostConfigurator hostConfigurator) {
		return new ThriftCluster(clusterName, hostConfigurator);
	}

	private void addKeyspaceIfMissing(int replicationFactor, Cluster cluster) {
		if (keyspaceExists(cluster)) {
			return;
		}
		KeyspaceDefinition newKeyspaceDefinition = hector.createKeyspaceDefinition(KEYSPACE_NAME, ThriftKsDef.DEF_STRATEGY_CLASS,
				replicationFactor, Collections.<ColumnFamilyDefinition>emptyList());
		cluster.addKeyspace(newKeyspaceDefinition, true);
	}

	private static boolean keyspaceExists(Cluster cluster) {
		KeyspaceDefinition keyspaceDefinition = cluster.describeKeyspace(KEYSPACE_NAME);
		return keyspaceDefinition != null;
	}

}

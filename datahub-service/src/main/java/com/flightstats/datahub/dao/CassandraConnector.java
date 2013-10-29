package com.flightstats.datahub.dao;

import me.prettyprint.hector.api.Cluster;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.Serializer;
import me.prettyprint.hector.api.ddl.ColumnFamilyDefinition;
import me.prettyprint.hector.api.exceptions.HInvalidRequestException;
import me.prettyprint.hector.api.mutation.Mutator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class CassandraConnector {

	private final static Logger logger = LoggerFactory.getLogger(CassandraConnector.class);
	final static String KEYSPACE_NAME = "DataHub";

	private final Cluster cluster;
	private final Keyspace keyspace;
	private final HectorFactoryWrapper hector;

	CassandraConnector(Cluster cluster, Keyspace keyspace, HectorFactoryWrapper hector) {
		this.cluster = cluster;
		this.keyspace = keyspace;
		this.hector = hector;
	}

	public <K> Mutator<K> buildMutator(Serializer<K> keySerializer) {
		return hector.createMutator(keyspace, keySerializer);
	}

	public boolean createColumnFamily(final String columnFamilyName) {
		return createColumnFamily(columnFamilyName, true);
	}

	public boolean createColumnFamily(final String columnFamilyName, boolean verbose) {
		ColumnFamilyDefinition columnFamilyDefinition = hector.createColumnFamilyDefinition(KEYSPACE_NAME, columnFamilyName);
		try {
//            columnFamilyDefinition.setMemtableThroughputInMb();
//            columnFamilyDefinition.setMemtableOperationsInMillions();
//            columnFamilyDefinition.setMemtableFlushAfterMins();

			cluster.addColumnFamily(columnFamilyDefinition, true);
			return true;
		} catch (HInvalidRequestException e) {
			if (verbose) {
				logger.warn("Error creating column family: " + e.getMessage(), e);
			}
			return false;
		}
	}

	public List<ColumnFamilyDefinition> getAllColumnFamilyDefinitions() {
		return cluster.describeKeyspace(KEYSPACE_NAME).getCfDefs();
	}

	public Keyspace getKeyspace() {
		return keyspace;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		CassandraConnector that = (CassandraConnector) o;

		if (cluster != null ? !cluster.equals(that.cluster) : that.cluster != null) {
			return false;
		}
		if (hector != null ? !hector.equals(that.hector) : that.hector != null) {
			return false;
		}
		if (keyspace != null ? !keyspace.equals(that.keyspace) : that.keyspace != null) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = cluster != null ? cluster.hashCode() : 0;
		result = 31 * result + (keyspace != null ? keyspace.hashCode() : 0);
		result = 31 * result + (hector != null ? hector.hashCode() : 0);
		return result;
	}
}

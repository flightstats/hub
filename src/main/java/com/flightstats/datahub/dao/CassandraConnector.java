package com.flightstats.datahub.dao;

import com.google.common.base.Predicate;
import me.prettyprint.hector.api.Cluster;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.Serializer;
import me.prettyprint.hector.api.ddl.ColumnFamilyDefinition;
import me.prettyprint.hector.api.mutation.Mutator;

import java.util.List;

import static com.google.common.collect.Iterators.any;

public class CassandraConnector {

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

    public boolean createColumnFamilyIfNeeded(final String columnFamilyName) {
        //Note: This is check-then-set and is technically incorrect.  Hector doesn't provide an atomic create-if-not-exists approach, so we
        //should eventually use a locking scheme here or let hector explode and catch the exception.
        if (columnFamilyExists(columnFamilyName)) {
            return false;
        }
        ColumnFamilyDefinition columnFamilyDefinition = hector.createColumnFamilyDefinition(KEYSPACE_NAME, columnFamilyName);
        cluster.addColumnFamily(columnFamilyDefinition, true);
        return true;
    }

    private boolean columnFamilyExists(final String columnFamilyName) {
        Predicate<ColumnFamilyDefinition> sameName = new Predicate<ColumnFamilyDefinition>() {
            @Override
            public boolean apply(ColumnFamilyDefinition input) {
                return input.getName().equals(columnFamilyName);
            }
        };
        List<ColumnFamilyDefinition> columnFamilyDefinitions = getAllColumnFamilyDefinitions();
        return any(columnFamilyDefinitions.iterator(), sameName);
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

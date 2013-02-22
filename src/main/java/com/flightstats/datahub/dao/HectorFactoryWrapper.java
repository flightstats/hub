package com.flightstats.datahub.dao;

import com.flightstats.datahub.model.DataHubCompositeValue;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.hector.api.Cluster;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.Serializer;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.ddl.ColumnFamilyDefinition;
import me.prettyprint.hector.api.ddl.KeyspaceDefinition;
import me.prettyprint.hector.api.factory.HFactory;
import me.prettyprint.hector.api.mutation.Mutator;
import me.prettyprint.hector.api.query.ColumnQuery;
import me.prettyprint.hector.api.query.SliceQuery;

import java.util.List;

/**
 * Let's hide the fact that HFactory is all static methods.  :/
 */
public class HectorFactoryWrapper {

    public KeyspaceDefinition createKeyspaceDefinition(String keyspaceName, String defStrategyClass, int replicationFactor, List<ColumnFamilyDefinition> columnFamilyDefinitions) {
        return HFactory.createKeyspaceDefinition(keyspaceName, defStrategyClass, replicationFactor, columnFamilyDefinitions);
    }

    public <K> Mutator<K> createMutator(Keyspace keyspace, Serializer<K> keySerializer) {
        return HFactory.createMutator(keyspace, keySerializer);
    }

    public ColumnFamilyDefinition createColumnFamilyDefinition(String keyspaceName, String columnFamilyName) {
        return HFactory.createColumnFamilyDefinition(keyspaceName, columnFamilyName);
    }

    public <K, V> HColumn<K, V> createColumn(K name, V value, Serializer<K> nameSerializer, Serializer<V> valueSerializer) {
        return HFactory.createColumn(name, value, nameSerializer, valueSerializer);
    }

    public <K, N, V> ColumnQuery<K, N, V> createColumnQuery(Keyspace keyspace, Serializer<K> keySerializer, Serializer<N> nameSerializer, Serializer<V> valueSerializer) {
        return HFactory.createColumnQuery(keyspace, keySerializer, nameSerializer, valueSerializer);
    }

    public Keyspace createKeyspace(String keyspaceName, Cluster cluster) {
        return HFactory.createKeyspace(keyspaceName, cluster);
    }

    SliceQuery<String, String, DataHubCompositeValue> createSliceQuery(Keyspace keyspace, StringSerializer keySerializer, StringSerializer columnNameSerializer, Serializer<DataHubCompositeValue> valueSerializer) {
        return HFactory.createSliceQuery(keyspace, keySerializer, columnNameSerializer, valueSerializer);
    }
}

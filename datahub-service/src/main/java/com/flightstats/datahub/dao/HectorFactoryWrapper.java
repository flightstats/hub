package com.flightstats.datahub.dao;

import me.prettyprint.cassandra.service.ColumnSliceIterator;
import me.prettyprint.hector.api.Cluster;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.Serializer;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.ddl.ColumnFamilyDefinition;
import me.prettyprint.hector.api.ddl.KeyspaceDefinition;
import me.prettyprint.hector.api.factory.HFactory;
import me.prettyprint.hector.api.mutation.Mutator;
import me.prettyprint.hector.api.query.*;

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

	public <K, N, V> RangeSlicesQuery<K, N, V> createRangeSlicesQuery(Keyspace keyspace, Serializer<K> keySerializer, Serializer<N> nameSerializer, Serializer<V> valueSerializer) {
		return HFactory.createRangeSlicesQuery(keyspace, keySerializer, nameSerializer, valueSerializer);
	}

	public <K, N> CountQuery<K, N> createCountQuery(Keyspace keyspace, Serializer<K> keySerializer, Serializer<N> nameSerializer) {
		return HFactory.createCountQuery(keyspace, keySerializer, nameSerializer);
	}

	public <K, N, V> SliceQuery<K, N, V> createSliceQuery(Keyspace keyspace, Serializer<K> keySerializer, Serializer<N> nameSerializer, Serializer<V> valueSerializer) {
		return HFactory.createSliceQuery(keyspace, keySerializer, nameSerializer, valueSerializer);
	}

    public <K,N,V> MultigetSliceQuery<K, N, V> createMultiGetSliceQuery(Keyspace keyspace, Serializer<K> keySerializer, Serializer<N> nameSerializer, Serializer<V> valueSerializer){
        return HFactory.createMultigetSliceQuery(keyspace, keySerializer, nameSerializer, valueSerializer);
    }

	public <K, N, V> ColumnSliceIterator<K, N, V> createColumnSliceIterator(SliceQuery<K, N, V> query, N start, N finish, boolean reversed) {
		return new ColumnSliceIterator<>(query, start, finish, reversed);
	}
}
package com.flightstats.datahub.dao;

import me.prettyprint.cassandra.model.BasicColumnFamilyDefinition;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.hector.api.Cluster;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.ddl.ColumnFamilyDefinition;
import me.prettyprint.hector.api.ddl.KeyspaceDefinition;
import me.prettyprint.hector.api.mutation.Mutator;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.TestCase.assertSame;
import static org.mockito.Mockito.*;

public class CassandraConnectorTest {

    @Test
    public void testBuildMutator() throws Exception {
        HectorFactoryWrapper hector = mock(HectorFactoryWrapper.class);
        Keyspace keyspace = mock(Keyspace.class);
        Mutator<String> mutator = mock(Mutator.class);

        when(hector.createMutator(keyspace, StringSerializer.get())).thenReturn(mutator);

        CassandraConnector testClass = new CassandraConnector(null, keyspace, hector);

        Mutator<String> result = testClass.buildMutator(StringSerializer.get());
        assertSame(result, mutator);
    }

    @Test
    public void testCreateColumnFamily_exists() throws Exception {
        String keyspaceName = CassandraConnector.KEYSPACE_NAME;
        String columnFamilyName = "cfcf cf";

        Cluster cluster = mock(Cluster.class);
        ColumnFamilyDefinition columnFamilyDef = mock(ColumnFamilyDefinition.class);
        HectorFactoryWrapper hector = mock(HectorFactoryWrapper.class);

        KeyspaceDefinition keyspaceDefs = mock(KeyspaceDefinition.class);
        when(cluster.describeKeyspace(keyspaceName)).thenReturn(keyspaceDefs);
        when(columnFamilyDef.getName()).thenReturn(columnFamilyName);
        when(keyspaceDefs.getCfDefs()).thenReturn(Arrays.asList(columnFamilyDef));


        CassandraConnector testClass = new CassandraConnector(cluster, null, hector);
        testClass.createColumnFamilyIfNeeded(columnFamilyName);

        verify(hector, never()).createColumnFamilyDefinition(anyString(), anyString());
        verify(cluster, never()).addColumnFamily(any(ColumnFamilyDefinition.class));
    }

    @Test
    public void testCreateColumnFamily_doesntExists() throws Exception {
        String keyspaceName = CassandraConnector.KEYSPACE_NAME;
        String columnFamilyName = "cfcf cf";

        Cluster cluster = mock(Cluster.class);
        ColumnFamilyDefinition columnFamilyDef = mock(ColumnFamilyDefinition.class);
        HectorFactoryWrapper hector = mock(HectorFactoryWrapper.class);

        KeyspaceDefinition keyspaceDefs = mock(KeyspaceDefinition.class);
        when(cluster.describeKeyspace(keyspaceName)).thenReturn(keyspaceDefs);
        when(columnFamilyDef.getName()).thenReturn("existing is not yours");
        when(keyspaceDefs.getCfDefs()).thenReturn(Arrays.asList(columnFamilyDef));
        when(hector.createColumnFamilyDefinition(keyspaceName, columnFamilyName)).thenReturn(columnFamilyDef);

        CassandraConnector testClass = new CassandraConnector(cluster, null, hector);
        testClass.createColumnFamilyIfNeeded(columnFamilyName);

        verify(hector).createColumnFamilyDefinition(keyspaceName, columnFamilyName);
        verify(cluster).addColumnFamily(columnFamilyDef, true);
    }

    @Test
    public void testGetAllColumnFamilyDefinitions() throws Exception {
        ColumnFamilyDefinition columnFamilyDefinition1 = new BasicColumnFamilyDefinition();
        ColumnFamilyDefinition columnFamilyDefinition2 = new BasicColumnFamilyDefinition();
        List<ColumnFamilyDefinition> expected = Arrays.asList(columnFamilyDefinition1, columnFamilyDefinition2);

        Cluster cluster = mock(Cluster.class);
        KeyspaceDefinition keyspaceDefinition = mock(KeyspaceDefinition.class);


        when(cluster.describeKeyspace(CassandraConnector.KEYSPACE_NAME)).thenReturn(keyspaceDefinition);
        when(keyspaceDefinition.getCfDefs()).thenReturn(expected);

        CassandraConnector testClass = new CassandraConnector(cluster, null, null);

        List<ColumnFamilyDefinition> result = testClass.getAllColumnFamilyDefinitions();

        assertEquals(expected, result);
    }
}

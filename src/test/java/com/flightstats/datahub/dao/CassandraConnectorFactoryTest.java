package com.flightstats.datahub.dao;

import me.prettyprint.cassandra.service.CassandraHostConfigurator;
import me.prettyprint.cassandra.service.ThriftKsDef;
import me.prettyprint.hector.api.Cluster;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.ddl.ColumnFamilyDefinition;
import me.prettyprint.hector.api.ddl.KeyspaceDefinition;
import org.junit.Test;

import java.util.Collections;

import static com.flightstats.datahub.dao.CassandraConnector.KEYSPACE_NAME;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class CassandraConnectorFactoryTest {

    @Test
    public void testBuild_keyspaceMissing() throws Exception {
        HectorFactoryWrapper hector = mock(HectorFactoryWrapper.class);
        final Cluster cluster = mock(Cluster.class);
        Keyspace keyspace = mock(Keyspace.class);
        KeyspaceDefinition keyspaceDefinition = mock(KeyspaceDefinition.class);

        when(cluster.describeKeyspace(CassandraConnector.KEYSPACE_NAME)).thenReturn(null);
        when(hector.createKeyspace(KEYSPACE_NAME, cluster)).thenReturn(keyspace);
        when(hector.createKeyspaceDefinition(KEYSPACE_NAME, ThriftKsDef.DEF_STRATEGY_CLASS,
                4, Collections.<ColumnFamilyDefinition>emptyList())).thenReturn(keyspaceDefinition);

        CassandraConnectorFactory testClass = new CassandraConnectorFactory("name", "hphphp", 4, hector) {
            @Override
            Cluster getCluster(CassandraHostConfigurator hostConfigurator) {
                return cluster;
            }
        };

        CassandraConnector result = testClass.build();

        assertEquals(new CassandraConnector(cluster, keyspace, hector), result);
        verify(cluster).addKeyspace(keyspaceDefinition, true);
    }

    @Test
    public void testBuild_keyspaceExists() throws Exception {
        HectorFactoryWrapper hector = mock(HectorFactoryWrapper.class);
        final Cluster cluster = mock(Cluster.class);
        Keyspace keyspace = mock(Keyspace.class);
        KeyspaceDefinition keyspaceDefinition = mock(KeyspaceDefinition.class);

        when(cluster.describeKeyspace(CassandraConnector.KEYSPACE_NAME)).thenReturn(keyspaceDefinition);
        when(hector.createKeyspace(KEYSPACE_NAME, cluster)).thenReturn(keyspace);
        when(hector.createKeyspaceDefinition(KEYSPACE_NAME, ThriftKsDef.DEF_STRATEGY_CLASS,
                4, Collections.<ColumnFamilyDefinition>emptyList())).thenReturn(keyspaceDefinition);

        CassandraConnectorFactory testClass = new CassandraConnectorFactory("name", "hphphp", 4, hector) {
            @Override
            Cluster getCluster(CassandraHostConfigurator hostConfigurator) {
                return cluster;
            }
        };

        CassandraConnector result = testClass.build();

        assertEquals(new CassandraConnector(cluster, keyspace, hector), result);
        verify(cluster, never()).addKeyspace(any(KeyspaceDefinition.class), anyBoolean());
    }
}

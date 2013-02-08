package com.flightstats.datahub.dao;

import com.google.inject.Inject;
import com.google.inject.Provides;
import me.prettyprint.cassandra.service.CassandraHostConfigurator;
import me.prettyprint.cassandra.service.ThriftCluster;
import me.prettyprint.cassandra.service.ThriftKsDef;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.ddl.ColumnFamilyDefinition;
import me.prettyprint.hector.api.ddl.KeyspaceDefinition;
import me.prettyprint.hector.api.factory.HFactory;

import javax.inject.Named;
import java.util.Collections;

import static com.flightstats.datahub.dao.CassandraConnector.KEYSPACE_NAME;

public class CassandraConnectorFactory {

    private final String clusterName;
    private final String hostPort;
    private final int replicationFactor;

    @Inject
    public CassandraConnectorFactory(@Named("cassandra.cluster_name") String clusterName, @Named("cassandra.hostport") String hostPort,
                                     @Named("cassandra.replication_factor") int replicationFactor) {
        this.clusterName = clusterName;
        this.hostPort = hostPort;
        this.replicationFactor = replicationFactor;
    }

    @Provides
    public CassandraConnector build() {
        CassandraHostConfigurator hostConfigurator = new CassandraHostConfigurator(hostPort);
        ThriftCluster cluster = new ThriftCluster(clusterName, hostConfigurator);
        addKeyspaceIfMissing(replicationFactor, cluster);
        Keyspace keyspace = HFactory.createKeyspace(KEYSPACE_NAME, cluster);
        return new CassandraConnector(cluster, keyspace);
    }

    private static void addKeyspaceIfMissing(int replicationFactor, ThriftCluster cluster) {
        KeyspaceDefinition keyspaceDefinition = cluster.describeKeyspace(KEYSPACE_NAME);
        if (keyspaceDefinition == null) {
            KeyspaceDefinition newKeyspaceDefinition = HFactory.createKeyspaceDefinition(KEYSPACE_NAME, ThriftKsDef.DEF_STRATEGY_CLASS,
                    replicationFactor, Collections.<ColumnFamilyDefinition>emptyList());
            cluster.addKeyspace(newKeyspaceDefinition, true);
        }
    }

}

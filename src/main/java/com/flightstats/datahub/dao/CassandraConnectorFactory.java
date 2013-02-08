package com.flightstats.datahub.dao;

import com.google.inject.Inject;
import com.google.inject.Provides;
import me.prettyprint.cassandra.service.CassandraHostConfigurator;
import me.prettyprint.cassandra.service.ThriftCluster;
import me.prettyprint.cassandra.service.ThriftKsDef;
import me.prettyprint.hector.api.Cluster;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.ddl.ColumnFamilyDefinition;
import me.prettyprint.hector.api.ddl.KeyspaceDefinition;

import javax.inject.Named;
import java.util.Collections;

import static com.flightstats.datahub.dao.CassandraConnector.KEYSPACE_NAME;

public class CassandraConnectorFactory {

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
        CassandraHostConfigurator hostConfigurator = new CassandraHostConfigurator(hostPort);
        Cluster cluster = new ThriftCluster(clusterName, hostConfigurator);
        addKeyspaceIfMissing(replicationFactor, cluster);
        Keyspace keyspace = hector.createKeyspace(KEYSPACE_NAME, cluster);
        return new CassandraConnector(cluster, keyspace, hector);
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

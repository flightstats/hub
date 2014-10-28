package com.flightstats.hub.dao.cassandra;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Host;
import com.datastax.driver.core.Metadata;
import com.datastax.driver.core.Session;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CassandraConnectorFactory {
    private final static Logger logger = LoggerFactory.getLogger(CassandraConnectorFactory.class);
    private final String contactPoints;

    @Inject
    public CassandraConnectorFactory(@Named("cassandra.contactPoints") String contactPoints) {
        this.contactPoints = contactPoints;
        logger.info("using cassandra at " + contactPoints);
    }

    public Session getCassandraSession() {
        Cluster.Builder builder = Cluster.builder();
        String[] strings = contactPoints.split(",");
        for (String contactPoint : strings) {
            builder.addContactPoint(contactPoint);
        }
        Cluster cluster = builder.build();
        Metadata metadata = cluster.getMetadata();
        logger.info("connected to cluster " + metadata.getClusterName());
        for (Host host : metadata.getAllHosts()) {
            logger.info("datacenter: {}, host: {}, rack: {}", host.getDatacenter(), host.getAddress(), host.getRack());
        }
        return cluster.connect();
    }
}

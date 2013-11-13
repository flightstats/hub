package com.flightstats.datahub.app;

import org.apache.cassandra.exceptions.ConfigurationException;
import org.apache.thrift.transport.TTransportException;
import org.cassandraunit.utils.EmbeddedCassandraServerHelper;

import java.io.IOException;

/**
 * Use this to run an ephemeral embedded instance of Cassandra.
 * Useful for running integration tests locally
 */
public class DataHubCassandra {

    public static void main(String[] args) throws InterruptedException, TTransportException, ConfigurationException, IOException {
        EmbeddedCassandraServerHelper.startEmbeddedCassandra();
    }
}

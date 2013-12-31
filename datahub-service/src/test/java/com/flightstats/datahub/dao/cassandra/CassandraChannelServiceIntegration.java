package com.flightstats.datahub.dao.cassandra;

import com.flightstats.datahub.dao.ChannelServiceIntegration;
import org.cassandraunit.utils.EmbeddedCassandraServerHelper;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import java.io.IOException;
import java.util.Properties;

public class CassandraChannelServiceIntegration extends ChannelServiceIntegration {

    @BeforeClass
    public static void setupClass() throws Exception {
        EmbeddedCassandraServerHelper.startEmbeddedCassandra();
        Properties properties = new Properties();
        properties.put("cassandra.cluster_name", "data hub");
        properties.put("cassandra.host", "127.0.0.1");
        properties.put("cassandra.port", "9142");
        properties.put("cassandra.replication_factor", "1");
        properties.put("cassandra.gc_grace_seconds", "1");
        properties.put("backing.store", "cassandra");
        properties.put("hazelcast.conf.xml", "");
        finalStartup(properties);
    }


    @Override
    protected void verifyStartup() {
    }

    @AfterClass
    public static void teardownClass() throws IOException {
        tearDown();
    }
}

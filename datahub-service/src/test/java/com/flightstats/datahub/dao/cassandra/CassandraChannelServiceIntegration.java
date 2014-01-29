package com.flightstats.datahub.dao.cassandra;

import com.flightstats.datahub.app.DataHubMain;
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
        Properties properties = DataHubMain.loadProperties("useDefault");
        properties.put("backing.store", "cassandra");
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

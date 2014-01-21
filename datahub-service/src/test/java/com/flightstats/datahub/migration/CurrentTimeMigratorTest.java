package com.flightstats.datahub.migration;

import com.flightstats.datahub.app.config.GuiceContextListenerFactory;
import com.flightstats.datahub.dao.ChannelService;
import com.google.inject.Injector;
import org.apache.curator.test.TestingServer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.Properties;

/**
 *
 */
public class CurrentTimeMigratorTest {

    private static TestingServer testingServer;
    private static ChannelService service;

    @BeforeClass
    public static void setupClass() throws Exception {
        Properties properties = new Properties();
        properties.put("backing.store", "aws");
        properties.put("dynamo.endpoint", "localhost:8000");
        properties.put("aws.protocol", "HTTP");
        properties.put("aws.environment", "test");
        properties.put("dynamo.table_creation_wait_minutes", "5");
        properties.put("aws.credentials", "default");
        properties.put("hazelcast.conf.xml", "");
        properties.put("zookeeper.connection", "localhost:2181");
        testingServer = new TestingServer(2181);
        Injector injector = GuiceContextListenerFactory.construct(properties).getInjector();
        service = injector.getInstance(ChannelService.class);
    }

    @AfterClass
    public static void teardownClass() throws IOException {
        testingServer.stop();
    }

    @Test
    public void testProd() throws Exception {

        CurrentTimeMigrator migrator = new CurrentTimeMigrator(service, "datahub.svc.prod", "positionAsdiRaw");
        migrator.run();
    }
}

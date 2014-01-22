package com.flightstats.datahub.migration;

import com.flightstats.datahub.app.config.GuiceContextListenerFactory;
import com.flightstats.datahub.dao.ChannelService;
import com.google.inject.Injector;
import org.apache.curator.test.TestingServer;

import java.util.Properties;

/**
 *
 */
public class MigratorDriver {

    private static TestingServer testingServer;
    private static ChannelService service;
    private static ChannelUtils channelUtils;

    public static void main(String[] args) throws Exception {
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
        channelUtils = injector.getInstance(ChannelUtils.class);
    }

    public void runProd() throws Exception {

        ChannelMigrator migrator = new ChannelMigrator(service, "datahub.svc.prod", "positionAsdiRaw", channelUtils);
        migrator.run();
    }
}

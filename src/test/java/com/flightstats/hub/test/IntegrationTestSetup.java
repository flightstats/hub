package com.flightstats.hub.test;

import com.flightstats.hub.cluster.ZooKeeperState;
import com.flightstats.hub.config.DependencyInjection;
import com.flightstats.hub.config.binding.HubBindings;
import com.flightstats.hub.config.properties.AppProperties;
import com.flightstats.hub.config.properties.PropertiesLoader;
import com.flightstats.hub.config.properties.ZooKeeperProperties;
import com.flightstats.hub.config.server.HubServer;
import com.flightstats.hub.config.server.ZooKeeperTestServer;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;

@Slf4j
public class IntegrationTestSetup {

    private static IntegrationTestSetup integrationTestSetup;
    private static CuratorFramework curator;
    private static Injector injector;

    private IntegrationTestSetup() {
        try {
            ZooKeeperTestServer.start();
            PropertiesLoader propertiesLoader = loadProperties();
            curator = buildZooKeeperClient(propertiesLoader);

            injector = new DependencyInjection().init();

            HubServer hubServer = injector.getInstance(HubServer.class);
            hubServer.start();
        } catch (Exception e) {
            log.info("Problem while setting up integration test environment", e);
        }
    }

    public static synchronized IntegrationTestSetup run() {
        if (integrationTestSetup == null) {
            integrationTestSetup = new IntegrationTestSetup();
        }
        return integrationTestSetup;
    }

    private CuratorFramework buildZooKeeperClient(PropertiesLoader propertiesLoader) {
        if (curator == null) {
            curator = HubBindings.buildCurator(
                    new ZooKeeperState(),
                    new AppProperties(propertiesLoader),
                    new ZooKeeperProperties(propertiesLoader));
        }
        return curator;
    }

    private PropertiesLoader loadProperties() {
        PropertiesLoader propertiesLoader = PropertiesLoader.getInstance();
        propertiesLoader.load("useDefault");
        propertiesLoader.setProperty("hub.type", "aws");
        propertiesLoader.setProperty("aws.protocol", "HTTP");
        propertiesLoader.setProperty("s3.config.management.enabled", "false");
        return propertiesLoader;
    }

    public CuratorFramework getZookeeperClient() {
        return curator;
    }

    public <T> T getInstance(Class<T> type) {
        return injector.getInstance(type);
    }

    public <T> T getInstance(Class<T> type, String name) {
        return injector.getInstance(Key.get(type, Names.named(name)));
    }

    public <T> T getInstance(TypeLiteral<T> type, String name) {
        return injector.getInstance(Key.get(type, Names.named(name)));
    }
}

package com.flightstats.hub.test;

import com.flightstats.hub.app.HubMain;
import com.flightstats.hub.cluster.ZooKeeperState;
import com.flightstats.hub.config.binding.HubBindings;
import com.flightstats.hub.config.properties.AppProperties;
import com.flightstats.hub.config.properties.PropertiesLoader;
import com.flightstats.hub.config.properties.ZooKeeperProperties;
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
            startZooKeeperServer();
            curator = buildZooKeeperClient();
            injector = new HubMain().run(false);
        } catch (Exception e) {
            log.info("Problem while setting up integration test environment");
        }
    }

    public static synchronized IntegrationTestSetup run() {
        if (integrationTestSetup == null) {
            integrationTestSetup = new IntegrationTestSetup();
        }
        return integrationTestSetup;
    }

    private CuratorFramework buildZooKeeperClient() {
        if (curator == null) {
            PropertiesLoader propertiesLoader = loadProperties();
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
        return propertiesLoader;
    }

    private void startZooKeeperServer() throws Exception {
        ZooKeeperTestServer.start();
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

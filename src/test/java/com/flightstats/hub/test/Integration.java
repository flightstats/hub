package com.flightstats.hub.test;

import com.flightstats.hub.app.HubMain;
import com.flightstats.hub.app.HubProvider;
import com.flightstats.hub.cluster.ZooKeeperState;
import com.flightstats.hub.config.AppProperties;
import com.flightstats.hub.config.PropertiesLoader;
import com.flightstats.hub.config.ZookeeperProperties;
import com.flightstats.hub.config.binding.HubBindings;
import com.google.inject.Injector;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.test.TestingServer;

@Slf4j
public class Integration {

    private static TestingServer testingServer;
    private static Injector injector;
    private static CuratorFramework curator;

    public static void main(String[] args) throws Exception {
        startZooKeeper();
    }

    public static CuratorFramework startZooKeeper() throws Exception {
        return startZooKeeper(new ZooKeeperState());
    }

    public static synchronized CuratorFramework startZooKeeper(ZooKeeperState zooKeeperState) throws Exception {
        final PropertiesLoader propertiesLoader = PropertiesLoader.getInstance();
        propertiesLoader.load("useDefault");

        if (testingServer == null) {
            log.info("starting zookeeper");
            testingServer = new TestingServer(2181);
            curator = HubBindings.buildCurator(
                    zooKeeperState,
                    new AppProperties(propertiesLoader),
                    new ZookeeperProperties(propertiesLoader));
        } else {
            log.info("zookeeper already started");
        }
        return curator;
    }

    public static synchronized Injector startAwsHub() throws Exception {
        PropertiesLoader.getInstance().setProperty("spoke.ttlMinutes", "240");

        if (injector != null) {
            testingServer.restart();
            return injector;
        }
        startZooKeeper();
        PropertiesLoader.getInstance().setProperty("hub.type", "aws");
        new HubMain().startServer();
        injector = HubProvider.getInjector();
        return injector;
    }

}

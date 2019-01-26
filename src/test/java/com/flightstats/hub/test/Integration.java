package com.flightstats.hub.test;

import com.flightstats.hub.app.HubBindings;
import com.flightstats.hub.app.HubMain;
import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.app.HubProvider;
import com.flightstats.hub.cluster.ZooKeeperState;
import com.google.inject.Injector;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.test.TestingServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Integration {
    private final static Logger logger = LoggerFactory.getLogger(Integration.class);
    private static TestingServer testingServer;
    private static Injector injector;
    private static CuratorFramework curator;

    public static void main(String[] args) throws Exception {
        startZooKeeper();
    }

    public static synchronized CuratorFramework startZooKeeper() throws Exception {
        HubProperties.loadProperties("useDefault");
        if (testingServer == null) {
            logger.info("starting zookeeper");
            testingServer = new TestingServer(2181);
            curator = HubBindings.buildCurator("hub", "test", "localhost:2181", new ZooKeeperState());
        } else {
            logger.info("zookeeper already started");
        }
        return curator;
    }

    public static synchronized Injector startAwsHub() throws Exception {
        HubProperties.setProperty("spoke.ttlMinutes", "240");
        if (injector != null) {
            return injector;
        }
        startZooKeeper();
        HubProperties.setProperty("hub.type", "aws");
        new HubMain().startServer();
        injector = HubProvider.getInjector();
        return injector;
    }

}

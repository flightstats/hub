package com.flightstats.hub.test;

import com.flightstats.hub.app.GuiceContext;
import com.flightstats.hub.app.HubMain;
import com.flightstats.hub.cluster.ZooKeeperState;
import com.google.inject.Injector;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.test.TestingServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

/**
 *
 */
public class Integration {
    private final static Logger logger = LoggerFactory.getLogger(Integration.class);
    private static TestingServer testingServer;
    private static Injector injector;
    private static Properties properties;
    private static CuratorFramework curator;

    public static void main(String[] args) throws Exception {
        startZooKeeper();
    }

    public static synchronized CuratorFramework startZooKeeper() throws Exception {
        if (testingServer == null) {
            logger.info("starting zookeeper");
            testingServer = new TestingServer(2181);
            RetryPolicy retryPolicy = GuiceContext.HubCommonModule.buildRetryPolicy();
            curator = GuiceContext.HubCommonModule.buildCurator("hub", "test", "localhost:2181", retryPolicy, new ZooKeeperState());
        } else {
            logger.info("zookeeper already started");
        }
        return curator;
    }

    public static synchronized Injector startRealHub() throws Exception {
        if (injector != null) {
            return injector;
        }
        startZooKeeper();
        properties = HubMain.loadProperties("useDefault");
        HubMain.startServer(properties);
        injector = HubMain.getInjector();
        return injector;
    }

}

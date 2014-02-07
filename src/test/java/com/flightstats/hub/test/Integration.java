package com.flightstats.hub.test;

import com.flightstats.hub.app.HubMain;
import com.flightstats.jerseyguice.jetty.JettyServer;
import com.google.inject.Injector;
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

    public static synchronized void startZooKeeper() throws Exception {
        if (testingServer == null) {
            logger.info("starting zookeeper");
            testingServer = new TestingServer(2181);
        } else {
            logger.info("zookeeper already started");
        }
    }

    public static synchronized Injector startHub() throws Exception {
        if (injector != null) {
            return injector;
        }
        //todo - gfm - 2/6/14 - check if it is started
        Properties properties = HubMain.loadProperties("useDefault");
        JettyServer jettyServer = HubMain.startServer(properties);
        injector = HubMain.getInjector();
        return injector;
    }
}

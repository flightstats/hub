package com.flightstats.hub.test;

import com.flightstats.hub.app.HubMain;
import com.flightstats.hub.dao.aws.AwsModule;
import com.flightstats.hub.dao.memory.MemoryModule;
import com.flightstats.hub.service.HubHealthCheck;
import com.flightstats.hub.service.MemoryHealthCheck;
import com.google.inject.Injector;
import org.apache.curator.test.TestingServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.UUID;

/**
 *
 */
public class Integration {
    private final static Logger logger = LoggerFactory.getLogger(Integration.class);
    private static TestingServer testingServer;
    private static Injector injector;
    private static Properties properties;
    private static boolean memoryStarted = false;

    public static synchronized void startZooKeeper() throws Exception {
        if (testingServer == null) {
            logger.info("starting zookeeper");
            testingServer = new TestingServer(2181);
        } else {
            logger.info("zookeeper already started");
        }
    }

    public static synchronized Injector startRealHub() throws Exception {
        if (injector != null) {
            return injector;
        }
        startZooKeeper();
        properties = HubMain.loadProperties("useDefault");
        HubMain.startServer(properties, new AwsModule(properties), HubHealthCheck.class);
        injector = HubMain.getInjector();
        return injector;
    }

    public static synchronized void startMemoryHub() throws Exception {
        if (memoryStarted) {
            return;
        }
        memoryStarted = true;
        logger.info("starting up memoryHub");
        Properties properties = HubMain.loadProperties("useDefault");
        properties.setProperty("http.bind_port", "9999");
        HubMain.startServer(properties, new MemoryModule(properties), MemoryHealthCheck.class);
    }

    public static String getRandomChannel() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }

    public static Properties getProperties() {
        return properties;
    }
}

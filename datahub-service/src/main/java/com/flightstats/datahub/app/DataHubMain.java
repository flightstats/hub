package com.flightstats.datahub.app;

import com.conducivetech.services.common.util.PropertyConfiguration;
import com.conducivetech.services.common.util.constraint.ConstraintException;
import com.flightstats.datahub.app.config.GuiceContextListenerFactory;
import com.flightstats.jerseyguice.jetty.JettyConfig;
import com.flightstats.jerseyguice.jetty.JettyConfigImpl;
import com.flightstats.jerseyguice.jetty.JettyServer;
import com.google.inject.servlet.GuiceServletContextListener;
import org.apache.zookeeper.server.quorum.QuorumPeerMain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

/**
 * Main entry point for the data hub.  This is the main runnable class.
 */
public class DataHubMain {

    private static final Logger logger = LoggerFactory.getLogger(DataHubMain.class);

    public static void main(String[] args) throws Exception {
        final Properties properties = loadProperties(args);
        logger.info(properties.toString());

        startZookeeper(properties);

        JettyServer server = startServer(properties);

        final CountDownLatch latch = new CountDownLatch(1);
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                logger.info("Jetty Server shutting down...");
                latch.countDown();
            }
        });
        latch.await();
        server.halt();
        logger.info("Server shutdown complete.  Exiting application.");
    }

    private static void startZookeeper(final Properties properties) {
        new Thread(new Runnable() {
            @Override
            public void run() {

                String zkConfigFile = properties.getProperty("zookeeper.cfg", "");
                if ("singleNode".equals(zkConfigFile)) {
                    logger.warn("using single node config file");
                    zkConfigFile = DataHubMain.class.getResource("/zooSingleNode.cfg").getFile();
                }
                logger.info("using " + zkConfigFile);
                QuorumPeerMain.main(new String[]{zkConfigFile});
            }
        }).start();
    }

    public static JettyServer startServer(Properties properties) throws IOException, ConstraintException {
        final JettyConfig jettyConfig = new JettyConfigImpl(properties);
        final GuiceServletContextListener guice = GuiceContextListenerFactory.construct(properties);
        JettyServer server = new JettyServer(jettyConfig, guice);
        server.start();
        logger.info("Jetty server has been started.");
        return server;
    }

    private static Properties loadProperties(String[] args) throws IOException {
        if (args.length > 0) {
            return PropertyConfiguration.loadProperties(new File(args[0]), true, logger);
        }
        URL resource = DataHubMain.class.getResource("/default.properties");
        return PropertyConfiguration.loadProperties(resource, true, logger);
    }
}

package com.flightstats.datahub.app;

import com.conducivetech.services.common.util.PropertyConfiguration;
import com.conducivetech.services.common.util.constraint.ConstraintException;
import com.flightstats.datahub.app.config.GuiceContextListenerFactory;
import com.flightstats.datahub.dao.TimeIndex;
import com.flightstats.datahub.dao.s3.TimeIndexCoordinator;
import com.flightstats.jerseyguice.jetty.JettyConfig;
import com.flightstats.jerseyguice.jetty.JettyConfigImpl;
import com.flightstats.jerseyguice.jetty.JettyServer;
import com.google.inject.Injector;
import org.apache.zookeeper.server.quorum.QuorumPeerMain;
import org.jetbrains.annotations.NotNull;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * Main entry point for the data hub.  This is the main runnable class.
 */
public class DataHubMain {

    private static final Logger logger = LoggerFactory.getLogger(DataHubMain.class);

    public static void main(String[] args) throws Exception {
        final Properties properties = loadProperties(args);
        logger.info(properties.toString());

        //todo - gfm - 1/7/14 - setup ZK to be it's own process
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
                    logger.warn("**********************************************************");
                    logger.warn("*** using zookeeper single node config file");
                    logger.warn("**********************************************************");
                    zkConfigFile = DataHubMain.class.getResource("/zooSingleNode.cfg").getFile();
                }
                logger.info("using " + zkConfigFile);
                QuorumPeerMain.main(new String[]{zkConfigFile});
            }
        }).start();
    }

    public static JettyServer startServer(Properties properties) throws IOException, ConstraintException {
        JettyConfig jettyConfig = new JettyConfigImpl(properties);
        GuiceContextListenerFactory.DataHubGuiceServletContextListener guice = GuiceContextListenerFactory.construct(properties);
        JettyServer server = new JettyServer(jettyConfig, guice);
        server.start();
        logger.info("Jetty server has been started.");
        Injector injector = guice.getInjector();
        TimeIndexCoordinator timeIndexCoordinator = injector.getInstance(TimeIndexCoordinator.class);
        int offset = new Random().nextInt(60);
        Executors.newScheduledThreadPool(1, new ThreadFactory() {
            @NotNull
            @Override
            public Thread newThread(@NotNull Runnable r) {
                return new Thread(r, "TimeIndex" + TimeIndex.getHash(new DateTime()));
            }

        }).scheduleWithFixedDelay(timeIndexCoordinator, offset, 60, TimeUnit.SECONDS);
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

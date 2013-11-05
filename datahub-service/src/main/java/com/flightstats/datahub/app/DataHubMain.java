package com.flightstats.datahub.app;

import com.conducivetech.services.common.util.PropertyConfiguration;
import com.conducivetech.services.common.util.constraint.ConstraintException;
import com.flightstats.datahub.app.config.GuiceContextListenerFactory;
import com.flightstats.jerseyguice.jetty.JettyConfig;
import com.flightstats.jerseyguice.jetty.JettyConfigImpl;
import com.flightstats.jerseyguice.jetty.JettyServer;
import com.google.inject.servlet.GuiceServletContextListener;
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

    //todo - gfm - 11/4/13 - this needs to have a method pulled out for running in int tests
    public static void main(String[] args) throws Exception {

        JettyServer server = startServer(args);

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

    public static JettyServer startServer(String[] args) throws IOException, ConstraintException {
        Properties properties = loadProperties(args);
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

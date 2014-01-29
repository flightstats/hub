package com.flightstats.datahub.app;

import com.amazonaws.services.cloudfront.model.InvalidArgumentException;
import com.conducivetech.services.common.util.PropertyConfiguration;
import com.conducivetech.services.common.util.constraint.ConstraintException;
import com.flightstats.datahub.app.config.GuiceContextListenerFactory;
import com.flightstats.datahub.dao.timeIndex.TimeIndexCoordinator;
import com.flightstats.datahub.replication.Replicator;
import com.flightstats.jerseyguice.jetty.JettyConfig;
import com.flightstats.jerseyguice.jetty.JettyConfigImpl;
import com.flightstats.jerseyguice.jetty.JettyServer;
import com.google.inject.Injector;
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
        if (args.length == 0) {
            throw new InvalidArgumentException("DataHubMain requires a property filename, or 'useDefault'");
        }
        final Properties properties = loadProperties(args[0]);
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
                    warn("using zookeeper single node config file");
                    zkConfigFile = DataHubMain.class.getResource("/zooSingleNode.cfg").getFile();
                }
                logger.info("using " + zkConfigFile);
                QuorumPeerMain.main(new String[]{zkConfigFile});
            }
        }).start();
    }

    private static void warn(String message) {
        logger.warn("**********************************************************");
        logger.warn("*** " + message);
        logger.warn("**********************************************************");
    }

    public static JettyServer startServer(Properties properties) throws IOException, ConstraintException {
        JettyConfig jettyConfig = new JettyConfigImpl(properties);
        GuiceContextListenerFactory.DataHubGuiceServletContextListener guice = GuiceContextListenerFactory.construct(properties);
        JettyServer server = new JettyServer(jettyConfig, guice);
        server.start();
        logger.info("Jetty server has been started.");
        Injector injector = guice.getInjector();
        //todo - gfm - 1/27/14 - does it make sense to pull these into TypeListeners via Guice?
        injector.getInstance(TimeIndexCoordinator.class).startThread();
        injector.getInstance(Replicator.class).startThreads();
        return server;
    }

    public  static Properties loadProperties(String fileName) throws IOException {
        if (fileName.equals("useDefault")) {
            warn("using default properties file");
            URL resource = DataHubMain.class.getResource("/default.properties");
            return PropertyConfiguration.loadProperties(resource, true, logger);
        }
        return PropertyConfiguration.loadProperties(new File(fileName), true, logger);
    }
}

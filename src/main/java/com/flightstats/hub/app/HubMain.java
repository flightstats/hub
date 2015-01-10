package com.flightstats.hub.app;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Injector;
import org.apache.zookeeper.server.ServerConfig;
import org.apache.zookeeper.server.ZooKeeperServerMain;
import org.apache.zookeeper.server.quorum.QuorumPeerConfig;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

/**
 * Main entry point for the data hub.  This is the main runnable class.
 */
public class HubMain {

    private static final Logger logger = LoggerFactory.getLogger(HubMain.class);
    private static final DateTime startTime = new DateTime();
    private static Injector injector;

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            throw new UnsupportedOperationException("HubMain requires a property filename, or 'useDefault'");
        }
        HubProperties.loadProperties(args[0]);
        startZookeeperIfSingle();

        JettyServer server = startServer();

        final CountDownLatch latch = new CountDownLatch(1);
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                logger.info("Jetty Server shutting down...");
                latch.countDown();
            }
        });
        latch.await();
        server.halt();
        HubServices.stopAll();
        logger.info("Server shutdown complete.  Exiting application.");
    }

    public static JettyServer startServer() throws IOException {
        GuiceContext.HubGuiceServlet guice = GuiceContext.construct();
        injector = guice.getInjector();
        JettyServer server = new JettyServer(guice);
        HubServices.start(HubServices.TYPE.PRE_START);
        server.start();
        logger.info("Jetty server has been started.");
        HubServices.start(HubServices.TYPE.POST_START);
        return server;
    }

    private static void startZookeeperIfSingle() {
        new Thread(new Runnable() {
            @Override
            public void run() {

                String zkConfigFile = HubProperties.getProperty("zookeeper.cfg", "");
                if ("singleNode".equals(zkConfigFile)) {
                    startSingleZookeeper();
                }
            }

            private void startSingleZookeeper() {
                try {
                    warn("using zookeeper single node config file");
                    QuorumPeerConfig config = new QuorumPeerConfig();
                    Properties zkProperties = new Properties();
                    zkProperties.setProperty("clientPort", "2181");
                    zkProperties.setProperty("initLimit", "5");
                    zkProperties.setProperty("syncLimit", "2");
                    zkProperties.setProperty("maxClientCnxns", "0");
                    zkProperties.setProperty("tickTime", "2000");
                    Path tempZookeeper = Files.createTempDirectory("zookeeper_");
                    zkProperties.setProperty("dataDir", tempZookeeper.toString());
                    zkProperties.setProperty("dataLogDir", tempZookeeper.toString());
                    config.parseProperties(zkProperties);
                    ServerConfig serverConfig = new ServerConfig();
                    serverConfig.readFrom(config);

                    new ZooKeeperServerMain().runFromConfig(serverConfig);
                } catch (Exception e) {
                    logger.warn("unable to start zookeeper", e);
                }
            }
        }).start();
    }

    static void warn(String message) {
        logger.warn("**********************************************************");
        logger.warn("*** " + message);
        logger.warn("**********************************************************");
    }

    @VisibleForTesting
    public static Injector getInjector() {
        return injector;
    }

    public static DateTime getStartTime() {
        return startTime;
    }
}

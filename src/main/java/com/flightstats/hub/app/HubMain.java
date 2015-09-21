package com.flightstats.hub.app;

import com.google.inject.Injector;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

/**
 * Main entry point for the hub.  This is the main runnable class.
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
        start();
    }

    static void start() throws IOException, InterruptedException {
        startZookeeperIfSingle();

        HubJettyServer server = startServer();

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

    public static HubJettyServer startServer() throws IOException {
        GuiceContext.HubGuiceServlet guice = GuiceContext.construct();
        injector = guice.getInjector();
        HubJettyServer server = new HubJettyServer(guice);
        HubServices.start(HubServices.TYPE.PRE_START);
        server.start();
        logger.info("Jetty server has been started.");
        HubServices.start(HubServices.TYPE.INITIAL_POST_START);
        logger.info("completed initial post start");
        HubServices.start(HubServices.TYPE.FINAL_POST_START);
        return server;
    }

    private static void startZookeeperIfSingle() {
        new Thread(() -> {
            String zkConfigFile = HubProperties.getProperty("runSingleZookeeperInternally", "");
            if ("singleNode".equals(zkConfigFile)) {
                warn("using single node zookeeper");
                ZookeeperMain.start();
            }
        }).start();
    }

    static void warn(String message) {
        logger.warn("**********************************************************");
        logger.warn("*** " + message);
        logger.warn("**********************************************************");
    }

    public static Injector getInjector() {
        return injector;
    }

    public static DateTime getStartTime() {
        return startTime;
    }
}

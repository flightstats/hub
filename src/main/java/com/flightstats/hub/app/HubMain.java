package com.flightstats.hub.app;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.flightstats.hub.filter.CORSFilter;
import com.flightstats.hub.filter.StreamEncodingFilter;
import com.flightstats.hub.metrics.InfluxdbReporterLifecycle;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import org.glassfish.jersey.message.DeflateEncoder;
import org.glassfish.jersey.message.GZipEncoder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.filter.EncodingFilter;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.Security;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * Main entry point for the hub.  This is the main runnable class.
 */
public class HubMain {

    private static final Logger logger = LoggerFactory.getLogger(HubMain.class);
    private static final DateTime startTime = new DateTime();

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            throw new UnsupportedOperationException("HubMain requires a property filename, 'useDefault', or 'useEncryptedDefault'");
        }
        HubProperties.loadProperties(args[0]);
        start();
    }

    static void start() throws Exception {
        Security.setProperty("networkaddress.cache.ttl", "60");
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
        logger.warn("calling shutdown");
        HubProvider.getInstance(ShutdownManager.class).shutdown(true);
        server.halt();
        logger.info("Server shutdown complete.  Exiting application.");
    }

    public static HubJettyServer startServer() throws IOException {
        ResourceConfig resourceConfig = new ResourceConfig();
        resourceConfig.register(new ObjectMapperResolver(HubBindings.objectMapper()));
        resourceConfig.register(JacksonJsonProvider.class);
        resourceConfig.registerClasses(CORSFilter.class,EncodingFilter.class, StreamEncodingFilter.class, GZipEncoder
                        .class,
                DeflateEncoder.class);

        List<Module> modules = new ArrayList<>();
        modules.add(new HubBindings());
        String hubType = HubProperties.getProperty("hub.type", "aws");
        logger.info("starting with hub.type {}", hubType);
        resourceConfig.packages("com.flightstats.hub");
        switch (hubType) {
            case "aws":
                modules.add(new ClusterHubBindings());
                break;
            case "nas":
            case "test":
                modules.add(new SingleHubBindings());
                break;
            default:
                throw new RuntimeException("unsupported hub.type " + hubType);
        }
        Injector injector = Guice.createInjector(modules);
        InfluxdbReporterLifecycle influxdbReporterLifecycle = injector.getInstance(InfluxdbReporterLifecycle.class);
        HubServices.register(influxdbReporterLifecycle, HubServices.TYPE.BEFORE_HEALTH_CHECK);
        HubProvider.setInjector(injector);
        HubServices.start(HubServices.TYPE.BEFORE_HEALTH_CHECK);
        HubJettyServer server = new HubJettyServer();
        server.start(resourceConfig);
        logger.info("Hub server has been started.");

        HubServices.start(HubServices.TYPE.PERFORM_HEALTH_CHECK);
        logger.info("completed initial post start");
        HubServices.start(HubServices.TYPE.AFTER_HEALTHY_START);
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

    public static DateTime getStartTime() {
        return startTime;
    }
}

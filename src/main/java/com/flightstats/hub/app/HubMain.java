package com.flightstats.hub.app;

import com.flightstats.hub.config.DependencyInjection;
import com.flightstats.hub.config.ServiceRegistration;
import com.flightstats.hub.config.properties.AppProperties;
import com.flightstats.hub.config.properties.PropertiesLoader;
import com.flightstats.hub.config.properties.ZooKeeperProperties;
import com.flightstats.hub.config.server.JettyServer;
import com.flightstats.hub.config.server.ZooKeeperTestServer;
import com.google.inject.Injector;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.server.Server;
import org.joda.time.DateTime;

import java.security.Security;
import java.util.concurrent.CountDownLatch;

@Slf4j
public class HubMain {

    private final ZooKeeperProperties zookeeperProperties = new ZooKeeperProperties(PropertiesLoader.getInstance());
    private final AppProperties appProperties = new AppProperties(PropertiesLoader.getInstance());
    private final StorageBackend storageBackend = StorageBackend.valueOf(appProperties.getHubType());
    private final static DateTime startTime = new DateTime();

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            throw new UnsupportedOperationException("HubMain requires a property filename, 'useDefault', or 'useEncryptedDefault'");
        }

        PropertiesLoader.getInstance().load(args[0]);
        new HubMain().run(true);
    }

    public static DateTime getStartTime() {
        return startTime;
    }

    public Injector run(boolean addshutDownHook) throws Exception {
        Security.setProperty("networkaddress.cache.ttl", "60");
        startZookeeperIfSingle();

        Injector injector = new DependencyInjection().init(storageBackend.toString());
        Server server = startServer(injector);

        if (addshutDownHook) {
            CountDownLatch latch = new CountDownLatch(1);
            addShutDownHook(latch);
            latch.await();

            log.warn("calling shutdown");
            injector.getInstance(ShutdownManager.class).shutdown(true);

            server.stop();
        }
        return injector;
    }

    public Server startServer(Injector injector) throws Exception {
        log.info("Hub server starting with hub.type {}", storageBackend.toString());

        new ServiceRegistration().register(storageBackend, injector);

        HubServices.start(HubServices.TYPE.BEFORE_HEALTH_CHECK);

        Server server = new JettyServer(injector).start();
        log.info("Hub server has been started.");

        HubServices.start(HubServices.TYPE.PERFORM_HEALTH_CHECK);
        log.info("Hub Server health check is complete");
        HubServices.start(HubServices.TYPE.AFTER_HEALTHY_START);
        log.info("Hub Server services have been started successfully");

        return server;
    }

    private void startZookeeperIfSingle() {
        try {
            String zkRunMode = zookeeperProperties.getZookeeperRunMode();
            if ("singleNode".equals(zkRunMode)) {
                ZooKeeperTestServer.start();
            }
        } catch (Exception e) {
            log.info("Problem starting zookeeper in single mode");
        }
    }

    private void addShutDownHook(CountDownLatch latch) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Jetty Server shutting down...");
            latch.countDown();
        }));
    }

}
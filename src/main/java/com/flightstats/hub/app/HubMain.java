package com.flightstats.hub.app;

import com.flightstats.hub.config.DependencyInjection;
import com.flightstats.hub.config.properties.PropertiesLoader;
import com.flightstats.hub.config.properties.ZooKeeperProperties;
import com.flightstats.hub.config.server.HubServer;
import com.flightstats.hub.config.server.ZooKeeperTestServer;
import com.google.common.html.HtmlEscapers;
import com.google.inject.Injector;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;

import java.util.concurrent.CountDownLatch;

@Slf4j
public class HubMain {

    private final static DateTime startTime = new DateTime();

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            throw new UnsupportedOperationException("HubMain requires a property filename, 'useDefault', or 'useEncryptedDefault'");
        }
        String sanitizedFileName = HtmlEscapers.htmlEscaper().escape(args[0]);
        PropertiesLoader.getInstance().load(sanitizedFileName);
        new HubMain().run();
    }

    public static DateTime getStartTime() {
        return startTime;
    }

    public void run() throws Exception {
        startZookeeperIfSingle();

        Injector injector = new DependencyInjection().init();
        HubServer hubServer = injector.getInstance(HubServer.class);
        hubServer.start();

        CountDownLatch latch = new CountDownLatch(1);
        addShutDownHook(latch);
        latch.await();

        hubServer.stop();
    }

    private void startZookeeperIfSingle() {
        ZooKeeperProperties zooKeeperProperties = new ZooKeeperProperties(PropertiesLoader.getInstance());
        try {
            if (zooKeeperProperties.isSingleServerModeEnabled()) {
                ZooKeeperTestServer.start();
            }
        } catch (Exception e) {
            log.error("Problem starting zookeeper in single mode");
        }
    }

    private void addShutDownHook(CountDownLatch latch) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Jetty Server shutting down...");
            latch.countDown();
        }));
    }

}
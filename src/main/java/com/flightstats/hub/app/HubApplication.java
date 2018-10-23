package com.flightstats.hub.app;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.flightstats.hub.filter.CORSFilter;
import com.flightstats.hub.filter.StreamEncodingFilter;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.jersey.message.DeflateEncoder;
import org.glassfish.jersey.message.GZipEncoder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.filter.EncodingFilter;

import javax.inject.Inject;
import java.security.Security;
import java.util.concurrent.CountDownLatch;

@Slf4j
public class HubApplication {

    private final ShutdownManager shutdownManager;
    private final HubJettyServer hubJettyServer;

    @Inject
    HubApplication(ShutdownManager shutdownManager, HubJettyServer hubJettyServer) {
        this.shutdownManager = shutdownManager;
        this.hubJettyServer = hubJettyServer;
    }

    public void run() throws Exception {
        Security.setProperty("networkaddress.cache.ttl", "60");

        HubServices.start(HubServices.TYPE.BEFORE_HEALTH_CHECK);
        hubJettyServer.start();
        log.info("Hub server has been started.");

        HubServices.start(HubServices.TYPE.PERFORM_HEALTH_CHECK);
        log.info("completed initial post start");

        HubServices.start(HubServices.TYPE.AFTER_HEALTHY_START);

        final CountDownLatch latch = new CountDownLatch(1);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Jetty Server shutting down...");
            latch.countDown();
        }));
        latch.await();
        log.warn("calling shutdown");
        shutdownManager.shutdown(true);
        hubJettyServer.halt();
        log.info("Server shutdown complete.  Exiting application.");
    }

}

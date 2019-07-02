package com.flightstats.hub.config.server;

import com.flightstats.hub.app.HubServices;
import com.flightstats.hub.app.ShutdownManager;
import com.flightstats.hub.config.ServiceRegistration;
import com.flightstats.hub.metrics.MetricsType;
import com.flightstats.hub.metrics.StatsdReporter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.server.Server;

import javax.inject.Inject;
import java.security.Security;

@Slf4j
public class HubServer {
    private final MetricsType HUB_STARTUP_METRIC = MetricsType.LIFECYCLE_STARTUP;

    private final ShutdownManager shutdownManager;
    private final ServiceRegistration serviceRegistration;
    private final JettyServer jettyServer;
    private final StatsdReporter statsdReporter;
    private Server server;

    @Inject
    public HubServer(ShutdownManager shutdownManager,
                     ServiceRegistration serviceRegistration,
                     JettyServer jettyServer,
                     StatsdReporter statsdReporter) {
        this.shutdownManager = shutdownManager;
        this.serviceRegistration = serviceRegistration;
        this.jettyServer = jettyServer;
        this.statsdReporter = statsdReporter;
    }

    public void start() throws Exception {
        Security.setProperty("networkaddress.cache.ttl", "60");

        statsdReporter.incrementEventStart(HUB_STARTUP_METRIC);
        serviceRegistration.register();
        HubServices.start(HubServices.TYPE.BEFORE_HEALTH_CHECK);
        server = jettyServer.start();
        log.info("Hub server has been started.");

        HubServices.start(HubServices.TYPE.PERFORM_HEALTH_CHECK);
        log.info("jHub Server health check is complete");
        HubServices.start(HubServices.TYPE.AFTER_HEALTHY_START);
        log.info("jHub Server services have been started successfully");
        statsdReporter.incrementEventCompletion(HUB_STARTUP_METRIC);
    }

    public void stop() throws Exception {
        log.warn("calling shutdown");
        shutdownManager.shutdown(true);
        server.stop();
    }
}



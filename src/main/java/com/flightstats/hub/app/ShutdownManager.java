package com.flightstats.hub.app;

import com.flightstats.hub.config.properties.AppProperties;
import com.flightstats.hub.config.properties.LocalHostProperties;
import com.flightstats.hub.health.HubHealthCheck;
import com.flightstats.hub.metrics.MetricNames;
import com.flightstats.hub.metrics.StatsdReporter;
import com.flightstats.hub.util.Sleeper;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.KeeperException;

import javax.inject.Inject;
import java.util.concurrent.Executors;

/**
 * ShutdownResource should only be called from the node's instance by the upstart prestop.sh script
 */
@SuppressWarnings("WeakerAccess")
@Singleton
@Slf4j
public class ShutdownManager {

    private static final String PATH = "/ShutdownManager";

    private final HubHealthCheck hubHealthCheck;
    private final CuratorFramework curatorFramework;
    private final StatsdReporter statsdReporter;
    private final AppProperties appProperties;
    private final String hostAddress;

    @Inject
    public ShutdownManager(HubHealthCheck hubHealthCheck,
                           CuratorFramework curatorFramework,
                           StatsdReporter statsdReporter,
                           AppProperties appProperties,
                           LocalHostProperties localHostProperties) {
        this.hubHealthCheck = hubHealthCheck;
        this.curatorFramework = curatorFramework;
        this.statsdReporter = statsdReporter;
        this.appProperties = appProperties;
        this.hostAddress = localHostProperties.getAddress();
        HubServices.register(new ShutdownManagerService(), HubServices.TYPE.AFTER_HEALTHY_START);
    }

    public boolean shutdown(boolean useLock) throws Exception {
        log.warn("shutting down!");
        String[] tags = {"restart", "shutdown"};
        statsdReporter.event("Hub Restart Shutdown", "shutting down", tags);
        reportEvent(MetricNames.LIFECYCLE_SHUTDOWN_START);
        statsdReporter.mute();

        if (hubHealthCheck.isShuttingDown()) {
            return true;
        }
        if (useLock) {
            waitForLock();
        }

        //this call will get the node removed from the Load Balancer
        hubHealthCheck.shutdown();
        long start = System.currentTimeMillis();
        HubServices.preStop();

        //wait until it's likely the node is removed from the Load Balancer
        long end = System.currentTimeMillis();
        int shutdownDelayInMiilis = appProperties.getShutdownDelayInMiilis();
        long millisStopping = end - start;
        if (millisStopping < shutdownDelayInMiilis) {
            long sleepTime = shutdownDelayInMiilis - millisStopping;

            log.warn("sleeping for " + sleepTime);
            Sleeper.sleep(sleepTime);
            log.warn("slept for " + sleepTime);
        }

        HubServices.stopAll();
        log.warn("completed shutdown tasks, exiting JVM");
        reportEvent(MetricNames.LIFECYCLE_SHUTDOWN_COMPLETE);
        Executors.newSingleThreadExecutor().submit(() -> System.exit(0));
        return true;
    }

    public String getLockData() throws Exception {
        byte[] bytes = curatorFramework.getData().forPath(PATH);
        return new String(bytes);
    }

    public boolean resetLock() throws Exception {
        try {
            log.info("resetting lock " + PATH);
            curatorFramework.delete().forPath(PATH);
            return true;
        } catch (KeeperException.NoNodeException e) {
            log.info("node not found for ..." + PATH);
            return false;
        }
    }

    private void waitForLock() throws Exception {
        while (true) {
            try {
                String lockData = getLockData();
                log.info("waiting for shutdown lock {}", lockData);
                Sleeper.sleep(1000);
            } catch (KeeperException.NoNodeException e) {
                log.info("creating shutdown lock");
                try {
                    curatorFramework.create().forPath(PATH, hostAddress.getBytes());
                    return;
                } catch (Exception e1) {
                    log.info("why did this fail?", e1);
                }
            }
        }
    }

    private void reportEvent(String eventName) {
        String[] tags = {"restart", "shutdown"};
        statsdReporter.incrementCounter(eventName, tags);
    }

    private class ShutdownManagerService extends AbstractIdleService {

        @Override
        protected void startUp() throws Exception {
            try {
                String foundIpAddress = getLockData();
                log.info("found shutdown lock {} local {}", foundIpAddress, hostAddress);
                if (hostAddress.equals(foundIpAddress)) {
                    log.info("deleting shutdown lock {} local {}", foundIpAddress, hostAddress);
                    resetLock();
                }
            } catch (KeeperException.NoNodeException e) {
                log.info("node not found for ..." + PATH);
            }
        }

        @Override
        protected void shutDown() {
            //do nothing, ShutdownManager should have already been shutdown.
        }
    }
}

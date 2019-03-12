package com.flightstats.hub.app;

import com.flightstats.hub.health.HubHealthCheck;
import com.flightstats.hub.metrics.StatsdReporter;
import com.flightstats.hub.util.Sleeper;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;

/**
 * ShutdownResource should only be called from the node's instance by the upstart prestop.sh script
 */
@SuppressWarnings("WeakerAccess")
@Singleton
@Slf4j
public class ShutdownManager {

    private static final String PATH = "/ShutdownManager";
    private StatsdReporter statsdReporter;

    @Inject
    public ShutdownManager(StatsdReporter statsdReporter) {
        this.statsdReporter = statsdReporter;
        HubServices.register(new ShutdownManagerService(), HubServices.TYPE.AFTER_HEALTHY_START);
    }

    public boolean shutdown(boolean useLock) throws Exception {
        log.warn("shutting down!");
        String[] tags = { "restart", "shutdown" };
        statsdReporter.event("Hub Restart Shutdown", "shutting down", tags);
        statsdReporter.mute();

        HubHealthCheck healthCheck = HubProvider.getInstance(HubHealthCheck.class);
        if (healthCheck.isShuttingDown()) {
            return true;
        }
        if (useLock) {
            waitForLock();
        }

        //this call will get the node removed from the Load Balancer
        healthCheck.shutdown();
        long start = System.currentTimeMillis();
        HubServices.preStop();

        //wait until it's likely the node is removed from the Load Balancer
        long end = System.currentTimeMillis();
        int shutdown_delay_millis = HubProperties.getProperty("app.shutdown_delay_seconds", 60) * 1000;
        long millisStopping = end - start;
        if (millisStopping < shutdown_delay_millis) {
            long sleepTime = shutdown_delay_millis - millisStopping;
            log.warn("sleeping for " + sleepTime);
            Sleeper.sleep(sleepTime);
            log.warn("slept for " + sleepTime);
        }

        HubServices.stopAll();
        log.warn("completed shutdown tasks, exiting JVM");
        Executors.newSingleThreadExecutor().submit(() -> System.exit(0));
        return true;
    }

    public String getLockData() throws Exception {
        byte[] bytes = getCurator().getData().forPath(PATH);
        return new String(bytes);
    }

    public boolean resetLock() throws Exception {
        try {
            log.info("resetting lock " + PATH);
            getCurator().delete().forPath(PATH);
            return true;
        } catch (KeeperException.NoNodeException e) {
            log.info("node not found for ..." + PATH);
            return false;
        }
    }

    private CuratorFramework getCurator() {
        return HubProvider.getInstance(CuratorFramework.class);
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
                    getCurator().create().forPath(PATH, HubHost.getLocalAddress().getBytes());
                    return;
                } catch (Exception e1) {
                    log.info("why did this fail?", e1);
                }
            }
        }
    }

    private class ShutdownManagerService extends AbstractIdleService {

        @Override
        protected void startUp() throws Exception {
            try {
                String foundIpAddress = getLockData();
                log.info("found shutdown lock {} local {}", foundIpAddress, HubHost.getLocalAddress());
                if (HubHost.getLocalAddress().equals(foundIpAddress)) {
                    log.info("deleting shutdown lock {} local {}", foundIpAddress, HubHost.getLocalAddress());
                    resetLock();
                }
            } catch (KeeperException.NoNodeException e) {
                log.info("node not found for ..." + PATH);
            }
        }

        @Override
        protected void shutDown() throws Exception {
            //do nothing, ShutdownManager should have already been shutdown.
        }
    }
}

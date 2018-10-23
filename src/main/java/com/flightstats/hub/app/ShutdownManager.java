package com.flightstats.hub.app;

import com.flightstats.hub.health.HubHealthCheck;
import com.flightstats.hub.metrics.MetricsService;
import com.flightstats.hub.util.Sleeper;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Singleton;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.concurrent.Executors;

/**
 * ShutdownResource should only be called from the node's instance by the upstart prestop.sh script
 */
@Singleton
public class ShutdownManager {

    private final static Logger logger = LoggerFactory.getLogger(ShutdownManager.class);

    private static final String PATH = "/ShutdownManager";
    private final HubHealthCheck healthCheck;
    private final int shutdownDelayMS;
    private final MetricsService metricsService;
    private final CuratorFramework curatorFramework;

    @Inject
    public ShutdownManager(HubHealthCheck healthCheck, HubProperties hubProperties, MetricsService metricsService, CuratorFramework curatorFramework) {
        this.healthCheck = healthCheck;
        this.shutdownDelayMS = hubProperties.getProperty("app.shutdown_delay_seconds", 60) * 1000;
        this.metricsService = metricsService;
        this.curatorFramework = curatorFramework;

        HubServices.register(new ShutdownManagerService(), HubServices.TYPE.AFTER_HEALTHY_START);
    }

    public boolean shutdown(boolean useLock) throws Exception {
        logger.warn("shutting down!");
        metricsService.mute();

        if (healthCheck.isShuttingDown()) {
            return true;
        }
        if (useLock) {
            waitForLock();
        }
        metricsService.event("Hub Restart Shutdown", "shutting down", "restart", "shutdown");

        //this call will get the node removed from the Load Balancer
        healthCheck.shutdown();
        long start = System.currentTimeMillis();
        HubServices.preStop();

        //wait until it's likely the node is removed from the Load Balancer
        long end = System.currentTimeMillis();
        long millisStopping = end - start;
        if (millisStopping < shutdownDelayMS) {
            long sleepTime = shutdownDelayMS - millisStopping;
            logger.warn("sleeping for " + sleepTime);
            Sleeper.sleep(sleepTime);
            logger.warn("slept for " + sleepTime);
        }

        HubServices.stopAll();
        logger.warn("completed shutdown tasks, exiting JVM");
        Executors.newSingleThreadExecutor().submit(() -> System.exit(0));
        return true;
    }

    public String getLockData() throws Exception {
        byte[] bytes = curatorFramework.getData().forPath(PATH);
        return new String(bytes);
    }

    public boolean resetLock() throws Exception {
        try {
            logger.info("resetting lock " + PATH);
            curatorFramework.delete().forPath(PATH);
            return true;
        } catch (KeeperException.NoNodeException e) {
            logger.info("node not found for ..." + PATH);
            return false;
        }
    }

    private void waitForLock() throws Exception {
        while (true) {
            try {
                String lockData = getLockData();
                logger.info("waiting for shutdown lock {}", lockData);
                Sleeper.sleep(1000);
            } catch (KeeperException.NoNodeException e) {
                logger.info("creating shutdown lock");
                try {
                    curatorFramework.create().forPath(PATH, HubHost.getLocalAddress().getBytes());
                    return;
                } catch (Exception e1) {
                    logger.info("why did this fail?", e1);
                }
            }
        }
    }

    private class ShutdownManagerService extends AbstractIdleService {

        @Override
        protected void startUp() throws Exception {
            try {
                String foundIpAddress = getLockData();
                logger.info("found shutdown lock {} local {}", foundIpAddress, HubHost.getLocalAddress());
                if (HubHost.getLocalAddress().equals(foundIpAddress)) {
                    logger.info("deleting shutdown lock {} local {}", foundIpAddress, HubHost.getLocalAddress());
                    resetLock();
                }
            } catch (KeeperException.NoNodeException e) {
                logger.info("node not found for ..." + PATH);
            }
        }

        @Override
        protected void shutDown() {
            //do nothing, ShutdownManager should have already been shutdown.
        }
    }
}

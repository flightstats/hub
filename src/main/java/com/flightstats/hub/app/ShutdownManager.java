package com.flightstats.hub.app;

import com.flightstats.hub.health.HubHealthCheck;
import com.flightstats.hub.util.Sleeper;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Singleton;
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
public class ShutdownManager {

    private final static Logger logger = LoggerFactory.getLogger(ShutdownManager.class);

    private static final String PATH = "/ShutdownManager";

    public ShutdownManager() {
        HubServices.register(new ShutdownManagerService(), HubServices.TYPE.AFTER_HEALTHY_START);
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
                logger.warn("node not found for ..." + PATH);
            }
        }

        @Override
        protected void shutDown() throws Exception {
            //do nothing, ShutdownManager should have already been shutdown.
        }
    }

    public boolean shutdown() throws Exception {
        HubHealthCheck healthCheck = HubProvider.getInstance(HubHealthCheck.class);
        if (healthCheck.isShuttingDown()) {
            return true;
        }
        waitForLock();

        logger.warn("shutting down!");
        //this call will get the node removed from the Load Balancer
        healthCheck.shutdown();

        HubServices.preStop();

        //wait until it's likely the node is removed from the Load Balancer
        int shutdown_delay_seconds = HubProperties.getProperty("app.shutdown_delay_seconds", 60);
        logger.warn("sleeping for " + shutdown_delay_seconds);
        Sleeper.sleep(shutdown_delay_seconds * 1000);

        HubServices.stopAll();
        logger.warn("completed shutdown tasks, exiting JVM");
        Executors.newSingleThreadExecutor().submit(() -> System.exit(0));
        return true;
    }

    public String getLockData() throws Exception {
        byte[] bytes = getCurator().getData().forPath(PATH);
        return new String(bytes);
    }

    public boolean resetLock() throws Exception {
        try {
            logger.info("resetting lock " + PATH);
            getCurator().delete().forPath(PATH);
            return true;
        } catch (KeeperException.NoNodeException e) {
            logger.info("node not found for ..." + PATH);
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
                logger.info("waiting for shutdown lock {}", lockData);
                Sleeper.sleep(1000);
            } catch (KeeperException.NoNodeException e) {
                logger.info("creating shutdown lock");
                try {
                    getCurator().create().forPath(PATH, HubHost.getLocalAddress().getBytes());
                    return;
                } catch (Exception e1) {
                    logger.info("why did this fail?", e1);
                }
            }
        }
    }
}

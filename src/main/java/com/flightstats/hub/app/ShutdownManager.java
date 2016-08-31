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
@Singleton
class ShutdownManager {

    private final static Logger logger = LoggerFactory.getLogger(ShutdownManager.class);

    private static final HubHealthCheck healthCheck = HubProvider.getInstance(HubHealthCheck.class);
    private final static CuratorFramework curator = HubProvider.getInstance(CuratorFramework.class);
    private static final String PATH = "/ShutdownManager";

    public ShutdownManager() {
        HubServices.register(new ShutdownManagerService(), HubServices.TYPE.AFTER_HEALTHY_START);
    }

    private class ShutdownManagerService extends AbstractIdleService {

        @Override
        protected void startUp() throws Exception {
            try {
                byte[] bytes = curator.getData().forPath(PATH);
                String foundIpAddress = new String(bytes);
                logger.info("found shutdown lock {} local {}", foundIpAddress, HubHost.getLocalAddress());
                if (HubHost.getLocalAddress().equals(foundIpAddress)) {
                    logger.info("deleting shutdown lock {} local {}", foundIpAddress, HubHost.getLocalAddress());
                    curator.delete().forPath(PATH);
                }
            } catch (KeeperException.NoNodeException e) {
                logger.warn("node not found for ..." + PATH);
            }
        }

        @Override
        protected void shutDown() throws Exception {
            //todo gfm - do nothing, ShutdownManager should have already been shutdown.
        }
    }

    public boolean shutdown() throws Exception {
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
        logger.warn("completed shutdown tasks");
        Executors.newSingleThreadExecutor().submit(() -> System.exit(0));
        return true;
    }

    private void waitForLock() throws Exception {
        while (true) {
            try {
                byte[] bytes = curator.getData().forPath(PATH);
                logger.info("waiting for shutdown lock {}", new String(bytes));
                Sleeper.sleep(1000);
            } catch (KeeperException.NoNodeException e) {
                logger.info("creating shutdown lock");
                try {
                    curator.create().forPath(PATH, HubHost.getLocalAddress().getBytes());
                    return;
                } catch (Exception e1) {
                    logger.info("why did this fail?", e1);
                }
            }
        }
    }
}

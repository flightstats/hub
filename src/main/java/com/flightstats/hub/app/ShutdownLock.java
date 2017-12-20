package com.flightstats.hub.app;

import com.flightstats.hub.util.Sleeper;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ShutdownLock {

    private static final Logger logger = LoggerFactory.getLogger(ShutdownLock.class);
    private static final CuratorFramework curator = HubProvider.getInstance(CuratorFramework.class);
    private static final String PATH = "/ShutdownManager";

    static void clearLock() {
        try {
            String foundIpAddress = getLockData();
            logger.info("found shutdown lock {} local {}", foundIpAddress, HubHost.getLocalAddress());
            if (HubHost.getLocalAddress().equals(foundIpAddress)) {
                logger.info("deleting shutdown lock {} local {}", foundIpAddress, HubHost.getLocalAddress());
                resetLock();
            }
        } catch (KeeperException.NoNodeException e) {
            logger.info("node not found for ..." + PATH);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static void waitForLock() throws Exception {
        while (true) {
            try {
                String lockData = getLockData();
                logger.info("waiting for shutdown lock {}", lockData);
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

    private static String getLockData() throws Exception {
        byte[] bytes = curator.getData().forPath(PATH);
        return new String(bytes);
    }

    private static void resetLock() throws Exception {
        try {
            logger.info("resetting lock " + PATH);
            curator.delete().forPath(PATH);
        } catch (KeeperException.NoNodeException e) {
            logger.info("node not found for ..." + PATH);
        }
    }
}

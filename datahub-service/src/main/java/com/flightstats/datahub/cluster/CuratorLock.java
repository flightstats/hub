package com.flightstats.datahub.cluster;

import com.google.inject.Inject;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.locks.InterProcessSemaphoreMutex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class CuratorLock {
    private final static Logger logger = LoggerFactory.getLogger(CuratorLock.class);

    private final CuratorFramework curator;
    private final ZooKeeperState zooKeeperState;

    @Inject
    public CuratorLock(CuratorFramework curator, ZooKeeperState zooKeeperState) {
        this.curator = curator;
        this.zooKeeperState = zooKeeperState;
    }

    /**
     * When you use this method, be sure to check shouldStopWorking to see if you've lost the lock.
     * runWithLock will not throw an exception up the stack.
     * todo - gfm - 1/28/14 - is the above statement true?
     */
    public void runWithLock(Lockable lockable, String lockPath, long time, TimeUnit timeUnit) {

        InterProcessSemaphoreMutex mutex = new InterProcessSemaphoreMutex(curator, lockPath);
        try {
            logger.debug("attempting acquire {}", lockPath);
            if (mutex.acquire(time, timeUnit)) {
                logger.debug("acquired {}", lockPath);
                lockable.runWithLock();
            } else {
                logger.debug("unable to acquire {} ", lockPath);
            }
        } catch (Exception e) {
            logger.warn("oh no! issue with " + lockPath, e);
        } finally {
            try {
                mutex.release();
            } catch (Exception e) {
                //ignore
            }
        }
    }

    /**
     * All users should handle this
     */
    public boolean shouldStopWorking() {
        return zooKeeperState.shouldStopWorking();
    }

    public boolean shouldKeepWorking() {
        return !shouldStopWorking();
    }

}

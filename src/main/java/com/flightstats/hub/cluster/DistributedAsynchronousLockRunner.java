package com.flightstats.hub.cluster;

import com.google.inject.Inject;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.locks.InterProcessSemaphoreMutex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * DistributedAsynchronousLockRunner is intended for long or short running processes.
 */
public class DistributedAsynchronousLockRunner {
    private final static Logger logger = LoggerFactory.getLogger(DistributedAsynchronousLockRunner.class);

    private final CuratorFramework curator;
    private final ExecutorService singleThreadExecutor;
    private final Leadership leadership;

    private String lockPath;
    private InterProcessSemaphoreMutex mutex;

    @Inject
    public DistributedAsynchronousLockRunner(CuratorFramework curator, ZooKeeperState zooKeeperState) {
        this(curator, zooKeeperState, null);
    }

    public DistributedAsynchronousLockRunner(CuratorFramework curator, ZooKeeperState zooKeeperState, String lockPath) {
        this.curator = curator;
        this.lockPath = lockPath;
        singleThreadExecutor = Executors.newSingleThreadExecutor();
        leadership = new Leadership(zooKeeperState);
    }

    public void setLockPath(String lockPath) {
        this.lockPath = lockPath;
    }

    public boolean runWithLock(Lockable lockable, long time, TimeUnit timeUnit) {
        mutex = new InterProcessSemaphoreMutex(curator, lockPath);
        try {
            logger.debug("attempting acquire {}", lockPath);
            if (mutex.acquire(time, timeUnit)) {
                leadership.setLeadership(true);
                logger.debug("acquired {} {}", lockPath, leadership.hasLeadership());
                singleThreadExecutor.submit(() -> {
                    try {
                        lockable.takeLeadership(leadership);
                    } catch (Exception e) {
                        logger.warn("we lost the lock " + lockPath, e);
                        leadership.setLeadership(false);
                    } finally {
                        release();
                    }
                });
                return true;
            } else {
                logger.debug("unable to acquire {} ", lockPath);
                return false;
            }
        } catch (Exception e) {
            logger.warn("oh no! issue with " + lockPath, e);
            leadership.setLeadership(false);
            release();
            return false;
        }
    }

    public void stopWorking() {
        leadership.setLeadership(false);
    }

    public void delete() {
        stopWorking();
        if (mutex != null) {
            release();
        }
        try {
            singleThreadExecutor.awaitTermination(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            logger.info("InterruptedException for " + lockPath, e);
        }
    }

    private void release() {
        try {
            mutex.release();
        } catch (IllegalStateException e) {
            logger.info("illegal state " + lockPath + " " + e.getMessage());
        } catch (Exception e) {
            logger.warn("issue releasing mutex for " + lockPath, e);
        }
    }

}

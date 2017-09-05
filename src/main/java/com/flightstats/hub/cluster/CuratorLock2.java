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
 * CuratorLock2 blends concepts from CuratorLock and CuratorLeader
 * It is intended for long or short running processes.
 */
public class CuratorLock2 {
    private final static Logger logger = LoggerFactory.getLogger(CuratorLock2.class);

    private final CuratorFramework curator;
    private final ZooKeeperState zooKeeperState;
    private final ExecutorService singleThreadExecutor;

    @Inject
    public CuratorLock2(CuratorFramework curator, ZooKeeperState zooKeeperState) {
        this.curator = curator;
        this.zooKeeperState = zooKeeperState;
        singleThreadExecutor = Executors.newSingleThreadExecutor();
    }

    public boolean runWithLock(Lockable lockable, String lockPath, long time, TimeUnit timeUnit) {
        InterProcessSemaphoreMutex mutex = new InterProcessSemaphoreMutex(curator, lockPath);
        LeadershipV2 leadershipV2 = new LeadershipV2(zooKeeperState);
        try {
            logger.debug("attempting acquire {}", lockPath);
            if (mutex.acquire(time, timeUnit)) {
                logger.debug("acquired {}", lockPath);
                leadershipV2.setLeadership(true);
                singleThreadExecutor.submit(() -> {
                    try {
                        lockable.takeLeadership(leadershipV2);
                    } catch (Exception e) {
                        logger.warn("we lost the lock " + lockPath, e);
                        leadershipV2.setLeadership(false);
                    }
                });
                return true;
            } else {
                logger.debug("unable to acquire {} ", lockPath);
                return false;
            }
        } catch (Exception e) {
            logger.warn("oh no! issue with " + lockPath, e);
            return false;
        } finally {
            leadershipV2.setLeadership(false);
            try {
                mutex.release();
            } catch (Exception e) {
                //ignore
            }
        }
    }

    public void delete(final String lockPath) {
        //deleting the path within a lock will cause Curator to log an error 'Lease already released', which can be ignored.
        //todo - gfm - fix this
        //runWithLock(() -> curator.delete().deletingChildrenIfNeeded().forPath(lockPath), lockPath, 1, TimeUnit.SECONDS);
    }

}

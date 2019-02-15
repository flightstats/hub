package com.flightstats.hub.cluster;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.locks.InterProcessSemaphoreMutex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

public class DistributedLeadershipLockManager {
    private final static Logger logger = LoggerFactory.getLogger(DistributedLeadershipLockManager.class);
    private final CuratorFramework curator;
    private final ZooKeeperState zooKeeperState;

    @Inject
    public DistributedLeadershipLockManager(CuratorFramework curator, ZooKeeperState zooKeeperState) {
        this.curator = curator;
        this.zooKeeperState = zooKeeperState;
    }

    public LeadershipLock buildLock(String lockPath) {
        InterProcessSemaphoreMutex mutex = new InterProcessSemaphoreMutex(curator, lockPath);
        Leadership leadership = new Leadership(zooKeeperState);

        return LeadershipLock.builder()
                .leadership(leadership)
                .mutex(mutex)
                .lockPath(lockPath)
                .build();
    }

    public boolean tryAcquireLock(LeadershipLock leadershipLock, long waitTimeout, TimeUnit waitTimeoutUnit) {
        Leadership leadership = leadershipLock.getLeadership();
        InterProcessSemaphoreMutex mutex = leadershipLock.getMutex();
        try {
            logger.debug("attempting acquire {}", leadershipLock.getLockPath());
            if (mutex.acquire(waitTimeout, waitTimeoutUnit)) {
                leadership.setLeadership(true);
                logger.debug("acquired {} {}", leadershipLock.getLockPath(), leadership.hasLeadership());
                return true;
            } else {
                logger.debug("unable to acquire {} ", leadershipLock.getLockPath());
            }
        } catch (Exception e) {
            logger.warn("oh no! issue with " + leadershipLock.getLockPath(), e);
            release(leadershipLock);
        }
        return false;
    }

    public void release(LeadershipLock lock) {
        lock.getLeadership().setLeadership(false);
        try {
            lock.getMutex().release();
        } catch (IllegalStateException e) {
            logger.info("illegal state " + lock.getLockPath() + " " + e.getMessage());
        } catch (Exception e) {
            logger.warn("issue releasing mutex for " + lock.getLockPath(), e);
        }
    }
}

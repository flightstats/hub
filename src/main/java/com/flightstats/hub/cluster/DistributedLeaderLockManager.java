package com.flightstats.hub.cluster;

import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.locks.InterProcessSemaphoreMutex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

@Slf4j
public class DistributedLeaderLockManager {
    private final CuratorFramework curator;
    private final ZooKeeperState zooKeeperState;

    @Inject
    public DistributedLeaderLockManager(CuratorFramework curator, ZooKeeperState zooKeeperState) {
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
            log.debug("attempting acquire {}", leadershipLock.getLockPath());
            if (mutex.acquire(waitTimeout, waitTimeoutUnit)) {
                leadership.setLeadership(true);
                log.debug("acquired {} {}", leadershipLock.getLockPath(), leadership.hasLeadership());
                return true;
            } else {
                log.warn("unable to acquire {} ", leadershipLock.getLockPath());
            }
        } catch (Exception e) {
            log.error("oh no! issue with " + leadershipLock.getLockPath(), e);
            release(leadershipLock);
        }
        return false;
    }

    public void release(LeadershipLock lock) {
        lock.getLeadership().setLeadership(false);
        try {
            lock.getMutex().release();
        } catch (IllegalStateException e) {
            log.warn("illegal state {} {}", lock.getLockPath(), e.getMessage());
        } catch (Exception e) {
            log.warn("issue releasing mutex for {}", lock.getLockPath(), e);
        }
    }
}

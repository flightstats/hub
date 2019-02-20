package com.flightstats.hub.cluster;

import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Slf4j
public class DistributedLockRunner {
    private final DistributedLeadershipLockManager leadershipLockManager;

    @Inject
    public DistributedLockRunner(DistributedLeadershipLockManager leadershipLockManager) {
        this.leadershipLockManager = leadershipLockManager;
    }

    public boolean runWithLock(Consumer<LeadershipLock> runnable, String leadershipLockPath, long time, TimeUnit timeUnit) {
        LeadershipLock leadershipLock = leadershipLockManager.buildLock(leadershipLockPath);
        if (leadershipLockManager.tryAcquireLock(leadershipLock, time, timeUnit)) {
            try {
                runnable.accept(leadershipLock);
                return true;
            } catch (Exception e) {
                log.warn("we lost the lock " + leadershipLockPath, e);
            } finally {
                leadershipLockManager.release(leadershipLock);
            }
        }
        return false;
    }

    public void delete(LeadershipLock leadershipLock) {
        leadershipLockManager.release(leadershipLock);
    }
}

package com.flightstats.hub.cluster;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * DistributedAsynchronousLockRunner is intended for long or short running processes.
 */
@Slf4j
public class DistributedAsynchronousLockRunner {
    private final DistributedLeadershipLockManager leadershipLockManager;
    private final ExecutorService executorService;

    private String lockPath;

    @Inject
    public DistributedAsynchronousLockRunner(DistributedLeadershipLockManager leadershipLockManager) {
        this(null, leadershipLockManager);
    }

    public DistributedAsynchronousLockRunner(String lockPath, DistributedLeadershipLockManager leadershipLockManager) {
        this.lockPath = lockPath;
        this.leadershipLockManager = leadershipLockManager;
        this.executorService = Executors.newSingleThreadExecutor();
    }

    public void setLockPath(String lockPath) {
        this.lockPath = lockPath;
    }

    public Optional<LeadershipLock> runWithLock(Lockable lockable, long time, TimeUnit timeUnit) {
        LeadershipLock leadershipLock = leadershipLockManager.buildLock(lockPath);
        if (leadershipLockManager.tryAcquireLock(leadershipLock, time, timeUnit)) {
            executorService.submit(() -> {
                try {
                    lockable.takeLeadership(leadershipLock.getLeadership());
                } catch (Exception e) {
                    log.warn("we lost the lock " + lockPath, e);
                } finally {
                    leadershipLockManager.release(leadershipLock);
                }
            });
            return Optional.of(leadershipLock);
        }
        return Optional.empty();
    }

    public void delete(LeadershipLock leadershipLock) {
        try {
            executorService.awaitTermination(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.info("InterruptedException for " + leadershipLock.getLockPath(), e);
        } finally {
            leadershipLockManager.release(leadershipLock);
        }
    }
}
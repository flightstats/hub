package com.flightstats.hub.cluster;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * DistributedAsyncLockRunner is intended for long or short running processes.
 */
@Slf4j
public class DistributedAsyncLockRunner {
    private final DistributedLeaderLockManager leadershipLockManager;
    private final ExecutorService executorService;

    private String lockPath;

    @Inject
    public DistributedAsyncLockRunner(DistributedLeaderLockManager leadershipLockManager) {
        this(null, leadershipLockManager);
    }

    public DistributedAsyncLockRunner(String lockPath, DistributedLeaderLockManager leadershipLockManager) {
        this.lockPath = lockPath;
        this.leadershipLockManager = leadershipLockManager;
        this.executorService = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder().setNameFormat("AsyncLockRunner-" + lockPath).build());
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
            executorService.shutdown();
            if (!executorService.awaitTermination(1, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
                executorService.awaitTermination(3, TimeUnit.SECONDS);
            }
        } catch (InterruptedException e) {
            log.info("InterruptedException for " + leadershipLock.getLockPath(), e);
            Thread.currentThread().interrupt();
        } finally {
            leadershipLockManager.release(leadershipLock);
        }
    }

}
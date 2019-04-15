package com.flightstats.hub.cluster;

import com.flightstats.hub.webhook.DLog;
import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

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
        this.executorService = Executors.newSingleThreadExecutor();
    }

    public void setLockPath(String lockPath) {
        this.lockPath = lockPath;
    }

    public Optional<LeadershipLock> runWithLock(Lockable lockable, long time, TimeUnit timeUnit) {
        DLog.log("DALR - runWIthLock");
        LeadershipLock leadershipLock = leadershipLockManager.buildLock(lockPath);
        if (leadershipLockManager.tryAcquireLock(leadershipLock, time, timeUnit)) {
            executorService.submit(() -> {
                try {
                    DLog.log("DALR - calling takeLeadership()");
                    lockable.takeLeadership(leadershipLock.getLeadership());
                } catch (Exception e) {
                    log.warn("we lost the lock " + lockPath, e);
                } finally {
                    leadershipLockManager.release(leadershipLock);
                    DLog.log("DALR - released leadership due to end of runWithLock");
                }
            });
            return Optional.of(leadershipLock);
        }
        return Optional.empty();
    }

    public void delete(LeadershipLock leadershipLock) {
        try {
            DLog.log("DALR - trying to release leadership lock due to delete");
            executorService.shutdown();
            if (!executorService.awaitTermination(1, TimeUnit.SECONDS)) {
                DLog.log("DALR - interrupting exec to release leadership lock");
                executorService.shutdownNow();
                executorService.awaitTermination(5, TimeUnit.SECONDS);
            }

        } catch (InterruptedException e) {
            log.info("InterruptedException for " + leadershipLock.getLockPath(), e);
        } finally {
            leadershipLockManager.release(leadershipLock);
            DLog.log("DALR - released leadership due to delete");
        }
    }
}
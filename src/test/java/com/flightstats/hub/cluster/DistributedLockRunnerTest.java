package com.flightstats.hub.cluster;

import com.flightstats.hub.test.Integration;
import org.apache.curator.framework.CuratorFramework;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static com.google.common.collect.Lists.newArrayList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class DistributedLockRunnerTest {
    private static DistributedLeaderLockManager lockManager;
    private static AtomicReference<List<String>> lockList;

    @BeforeAll
    static void setupCurator() throws Exception {
        ZooKeeperState zooKeeperState = new ZooKeeperState();
        CuratorFramework curator = Integration.startZooKeeper(zooKeeperState);
        lockManager = new DistributedLeaderLockManager(curator, zooKeeperState);
    }

    @BeforeEach
    void setup() {
        lockList = new AtomicReference<>(newArrayList());
    }

    @Test
    void test_runWithLock_executesTheRunnableCode() throws Exception {
        DistributedLockRunner distributedLockRunner = new DistributedLockRunner(lockManager);
        CountDownLatch latch = new CountDownLatch(1);

        Consumer<LeadershipLock> lockable = leadershipLock -> {
            doAThing(0, "lockable1", latch);
            assertTrue(leadershipLock.getLeadership().hasLeadership());
            assertTrue(leadershipLock.getMutex().isAcquiredInThisProcess());
        };

        boolean runWithLock = distributedLockRunner.runWithLock(lockable, "/LockPath", 1, TimeUnit.MILLISECONDS);

        assertTrue(runWithLock);

        latch.await(1, TimeUnit.MINUTES);
        assertEquals(newArrayList("lockable1"), lockList.get());
    }

    @Test
    void test_runWithLock_withTwoLockables_runsOneAtATime() throws Exception {
        DistributedLockRunner distributedLockRunner = new DistributedLockRunner( lockManager);
        CountDownLatch waitForMe = new CountDownLatch(1);
        CountDownLatch latch = new CountDownLatch(2);

        ExecutorService executorService = Executors.newFixedThreadPool(3);
        executorService.execute(() -> {
            Consumer<LeadershipLock> lockable = leadershipLock -> {
                waitForMe.countDown();
                doAThing(100, "lockable1", latch);
            };
            distributedLockRunner.runWithLock(lockable, "/LockPath", 1, TimeUnit.MILLISECONDS);
        });

        executorService.execute(() -> {
            Consumer<LeadershipLock> lockable = leadershipLock -> doAThing(0, "lockable2", latch);
            tryWait(90, waitForMe);
            distributedLockRunner.runWithLock(lockable, "/LockPath", 1, TimeUnit.MINUTES);
        });

        latch.await(5, TimeUnit.MINUTES);
        assertEquals(newArrayList("lockable1", "lockable2"), lockList.get());
    }

    @Test
    void test_runWithLock_afterAFailureToLock_continuesLocking() throws Exception {
        DistributedLockRunner distributedLockRunner = new DistributedLockRunner(lockManager);
        CountDownLatch waitForMe = new CountDownLatch(1);
        CountDownLatch latch = new CountDownLatch(2);

        ExecutorService executorService = Executors.newFixedThreadPool(3);
        executorService.execute(() -> {
            Consumer<LeadershipLock> lockable = leadershipLock -> {
                waitForMe.countDown();
                doAThing(100, "lockable1", latch);
            };
            distributedLockRunner.runWithLock(lockable, "/LockPath", 1, TimeUnit.MILLISECONDS);
        });

        executorService.execute(() -> {
            Consumer<LeadershipLock> lockable = leadershipLock -> doAThing(0, "lockable2", latch);
            tryWait(50, waitForMe);
            distributedLockRunner.runWithLock(lockable, "/LockPath", 1, TimeUnit.MILLISECONDS);
        });

        executorService.execute(() -> {
            Consumer<LeadershipLock> lockable = leadershipLock -> doAThing(0, "lockable3", latch);
            distributedLockRunner.runWithLock(lockable, "/LockPath", 1, TimeUnit.MINUTES);
        });

        latch.await(5, TimeUnit.MINUTES);
        assertTrue(lockList.get().contains("lockable1"));
        assertTrue(lockList.get().contains("lockable3"));
        assertEquals(2, lockList.get().size());
    }

    @Test
    void test_runWithLock_withTwoSeparateLocks_isAbleToLockOnBothPaths() {
        DistributedLockRunner distributedLockRunner = new DistributedLockRunner(lockManager);
        CountDownLatch waitForOne = new CountDownLatch(1);
        CountDownLatch waitForTwo = new CountDownLatch(1);
        CountDownLatch latch = new CountDownLatch(3);

        ExecutorService executorService = Executors.newFixedThreadPool(3);
        executorService.execute(() -> {
            Consumer<LeadershipLock> lockable = leadershipLock -> {
                waitForOne.countDown();
                doAThing(100, "lockable1", latch);
                tryWait(50, waitForTwo);
            };
            distributedLockRunner.runWithLock(lockable, "/LockPath", 1, TimeUnit.MILLISECONDS);
        });

        executorService.execute(() -> {
            Consumer<LeadershipLock> lockable = leadershipLock -> {
                doAThing(0, "lockable2", latch);
                waitForTwo.countDown();
            };
            tryWait(50, waitForOne);
            distributedLockRunner.runWithLock(lockable, "/LockPath2", 1, TimeUnit.MILLISECONDS);
        });

        executorService.execute(() -> {
            Consumer<LeadershipLock> lockable = leadershipLock -> doAThing(0, "lockable3", latch);
            tryWait(50, waitForOne);
            distributedLockRunner.runWithLock(lockable, "/LockPath", 1, TimeUnit.MINUTES);
        });

        try {
            latch.await(2, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            fail("something went wrong waiting for all the locks to finish!");
        }
        assertEquals(newArrayList("lockable2", "lockable1", "lockable3"), lockList.get());
    }

    private void tryWait(long sleepMillis, CountDownLatch latch) {
        try { latch.await(sleepMillis, TimeUnit.MILLISECONDS); }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail(e.getMessage());
        }
    }

    private void doAThing(long sleepMillis, String name, CountDownLatch latch) {
        try {
            Thread.sleep(sleepMillis);
        } catch (Exception e) {
            fail(e.getMessage());
        }

        List<String> newLockList = lockList.get();
        newLockList.add(name);

        lockList.set(newLockList);

        latch.countDown();
    }
}

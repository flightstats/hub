package com.flightstats.hub.cluster;

import com.flightstats.hub.test.Integration;
import com.google.common.collect.Range;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static junit.framework.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;


@Slf4j
@Execution(ExecutionMode.SAME_THREAD)
class DistributedLockRunnerTest {

    private static DistributedLeaderLockManager lockManager;
    private ConcurrentHashMap<String, Range<DateTime>> lockTimes;
    private DistributedLockRunner distributedLockRunner;

    @BeforeAll
    static void setupCurator() throws Exception {
        ZooKeeperState zooKeeperState = new ZooKeeperState();
        CuratorFramework curator = Integration.startZooKeeper(zooKeeperState);
        lockManager = new DistributedLeaderLockManager(curator, zooKeeperState);
    }

    @BeforeEach
    void setup() {
        lockTimes = new ConcurrentHashMap<>();
        this.distributedLockRunner = new DistributedLockRunner(lockManager);
    }

    @Test
    void test_runWithLock_executesTheRunnableCode() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);

        Consumer<LeadershipLock> lockable = leadershipLock -> trackLock("lockable1", latch, () ->{
            sleep(0);
            assertTrue(leadershipLock.getLeadership().hasLeadership());
            assertTrue(leadershipLock.getMutex().isAcquiredInThisProcess());
        });

        boolean runWithLock = distributedLockRunner.runWithLock(lockable, "/LockPath", 1, TimeUnit.MILLISECONDS);

        assertTrue(runWithLock);

        latch.await(1, TimeUnit.MINUTES);
        assertLocksHappened("lockable1");
    }

    @Test
    void test_runWithLock_withTwoLockables_runsOneAtATime() throws Exception {
        CountDownLatch waitForMe = new CountDownLatch(1);
        CountDownLatch latch = new CountDownLatch(2);
        ExecutorService executorService = Executors.newFixedThreadPool(3);

        executorService.execute(() -> {
            Consumer<LeadershipLock> lockable = leadershipLock -> trackLock("lockable1", latch, () -> {
                waitForMe.countDown();
                sleep(100);
            });
            distributedLockRunner.runWithLock(lockable, "/LockPath", 1, TimeUnit.MILLISECONDS);
        });

        executorService.execute(() -> {
            Consumer<LeadershipLock> lockable = leadershipLock -> trackLock("lockable2", latch, () -> sleep(0));
            tryWait(90, waitForMe);
            distributedLockRunner.runWithLock(lockable, "/LockPath", 1, TimeUnit.MINUTES);
        });

        latch.await(5, TimeUnit.MINUTES);

        assertLocksHappened("lockable1", "lockable2");
        assertLocksOccurredSerially("lockable1", "lockable2");
    }

    @Test
    void test_runWithLock_afterAFailureToLock_continuesLocking() throws Exception {
        final CountDownLatch waitForMe = new CountDownLatch(1);
        final CountDownLatch latch = new CountDownLatch(2);
        final ExecutorService executorService = Executors.newFixedThreadPool(3);

        executorService.execute(() -> {
            Consumer<LeadershipLock> lockable = leadershipLock -> trackLock("lockable1", latch, () -> {
                waitForMe.countDown();
                sleep(100);
            });
            distributedLockRunner.runWithLock(lockable, "/LockPath", 1, TimeUnit.MINUTES);
        });

        executorService.execute(() -> {
            Consumer<LeadershipLock> lockable = leadershipLock -> trackLock("lockable2", latch, () -> {
                sleep(0);
            });
            tryWait(50, waitForMe);
            distributedLockRunner.runWithLock(lockable, "/LockPath", 1, TimeUnit.MILLISECONDS);
        });

        executorService.execute(() -> {
            Consumer<LeadershipLock> lockable = leadershipLock -> trackLock("lockable3", latch, () -> {
                sleep(0);
            });
            distributedLockRunner.runWithLock(lockable, "/LockPath", 1, TimeUnit.MINUTES);
        });

        latch.await(5, TimeUnit.MINUTES);
        assertLocksHappened("lockable1", "lockable3");
        assertFalse(locksOverlap("lockable1", "lockable3"));
    }

    @Test
    void test_runWithLock_withTwoSeparateLocks_isAbleToLockOnBothPaths() {
        final CountDownLatch waitForOne = new CountDownLatch(1);
        final CountDownLatch waitForTwo = new CountDownLatch(1);
        final CountDownLatch latch = new CountDownLatch(3);
        final ExecutorService executorService = Executors.newFixedThreadPool(3);
        executorService.execute(() -> {
            Consumer<LeadershipLock> lockable = leadershipLock -> trackLock("lockable1", latch, () -> {
                waitForOne.countDown();
                tryWait(50, waitForTwo);
                sleep(150);
            });

            distributedLockRunner.runWithLock(lockable, "/Lock", 1, TimeUnit.MINUTES);
        });

        executorService.execute(() -> {
            Consumer<LeadershipLock> lockable = leadershipLock -> trackLock("lockable2", latch, () -> {
                sleep(25);
                waitForTwo.countDown();
            });
            tryWait(50, waitForOne);
            distributedLockRunner.runWithLock(lockable, "/Lock2", 1, TimeUnit.MILLISECONDS);
        });

        executorService.execute(() -> {
            Consumer<LeadershipLock> lockable = leadershipLock -> trackLock("lockable3", latch, () -> {
                sleep(75);
            });
            tryWait(50, waitForTwo);
            distributedLockRunner.runWithLock(lockable, "/Lock", 1, TimeUnit.MINUTES);
        });

        try {
            latch.await(2, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            fail("something went wrong waiting for all the locks to finish!");
        }
        log.debug(lockTimes.toString());

        assertLocksHappened("lockable1", "lockable2", "lockable3");
        assertTrue(locksOverlap("lockable1", "lockable2") || locksOverlap("lockable2", "lockable3"));
        assertFalse(locksOverlap("lockable1", "lockable3"));
    }

    private void tryWait(long sleepMillis, CountDownLatch latch) {
        try {
            latch.await(sleepMillis, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("thread interrupted");
            fail(e.getMessage());
        }
    }

    private void trackLock(String name, CountDownLatch latch, Runnable doTheThing) {
        DateTime startTime = DateTime.now();
        log.debug("{} starting at {}", name, startTime);

        doTheThing.run();

        DateTime endTime = DateTime.now();
        log.debug("{} ending at {}", name, endTime);
        lockTimes.put(name, Range.closed(startTime, endTime));

        latch.countDown();
    }

    private void sleep(long sleepMillis) {
        try {
            Thread.sleep(sleepMillis);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    private boolean locksOverlap(String lockName1, String lockName2) {
        return lockTimes.get(lockName1).isConnected(lockTimes.get(lockName2));
    }

    private void assertLocksHappened(String... lockNames) {
        List<String> expectedLocks = Stream.of(lockNames).sorted().collect(toList());
        log.debug(lockTimes.toString());
        List<String> actualLocks = lockTimes.entrySet().stream().map(Map.Entry::getKey).sorted().collect(toList());
        assertEquals(expectedLocks, actualLocks);
    }

    private void assertLocksOccurredSerially(String lock1, String lock2) {
        DateTime lock1EndTime = lockTimes.get(lock1).upperEndpoint();
        DateTime lock2StartTime = lockTimes.get(lock2).lowerEndpoint();

        assertTrue(lock1EndTime.isBefore(lock2StartTime));
    }
}

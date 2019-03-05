package com.flightstats.hub.cluster;

import com.flightstats.hub.test.Integration;
import org.apache.curator.framework.CuratorFramework;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static com.google.common.collect.Lists.newArrayList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class DistributedAsyncLockRunnerTest {
    private static DistributedLeaderLockManager lockManager;
    private static AtomicReference<List<String>> lockList;

    @BeforeClass
    public static void setupCurator() throws Exception {
        ZooKeeperState zooKeeperState = new ZooKeeperState();
        CuratorFramework curator = Integration.startZooKeeper(zooKeeperState);
        lockManager = new DistributedLeaderLockManager(curator, zooKeeperState);
    }

    @Before
    public void setup() {
        lockList = new AtomicReference<>(newArrayList());
    }

    @Test
    public void testDoesWorkPassedInFromLockable() throws Exception {
        DistributedAsyncLockRunner distributedLockRunner = new DistributedAsyncLockRunner("/LockPath", lockManager);
        CountDownLatch latch = new CountDownLatch(1);
        SimpleTestLockable lockable = new SimpleTestLockable("lockable1", latch, 0);

        Optional<LeadershipLock> runWithLock = distributedLockRunner.runWithLock(lockable, 1, TimeUnit.MILLISECONDS);

        assertTrue(runWithLock.isPresent());

        latch.await(1, TimeUnit.MINUTES);

        assertEquals(newArrayList("lockable1"), lockList.get());
    }

    @Test
    public void testPreventsTwoLockablesFromRunningAtTheSameTime() throws Exception {
        DistributedAsyncLockRunner distributedLockRunner = new DistributedAsyncLockRunner("/LockPath2", lockManager);
        CountDownLatch latch = new CountDownLatch(2);
        SimpleTestLockable lockable = new SimpleTestLockable("lockable1", latch, 100);
        SimpleTestLockable secondLockable = new SimpleTestLockable("lockable2", latch, 0);

        Optional<LeadershipLock> run1WithLock = distributedLockRunner.runWithLock(lockable, 1, TimeUnit.MILLISECONDS);
        assertTrue(run1WithLock.isPresent());

        Optional<LeadershipLock> run2WithLock = distributedLockRunner.runWithLock(secondLockable, 1, TimeUnit.MINUTES);
        assertTrue(run2WithLock.isPresent());

        latch.await(5, TimeUnit.MINUTES);

        assertEquals(newArrayList("lockable1", "lockable2"), lockList.get());
    }

    @Test
    public void testIsAbleToContinueLockingAfterAFailureToLock() throws Exception {
        DistributedAsyncLockRunner distributedLockRunner = new DistributedAsyncLockRunner("/LockPath3", lockManager);
        CountDownLatch latch = new CountDownLatch(2);
        SimpleTestLockable lockable = new SimpleTestLockable("lockable1", latch, 1000);
        SimpleTestLockable secondLockable = new SimpleTestLockable("lockable2", latch, 0);

        SimpleTestLockable thirdLockable = new SimpleTestLockable("lockable3", latch, 0);

        Optional<LeadershipLock> run1WithLock = distributedLockRunner.runWithLock(lockable, 1, TimeUnit.MILLISECONDS);
        assertTrue(run1WithLock.isPresent());

        Optional<LeadershipLock> run2WithLock = distributedLockRunner.runWithLock(secondLockable, 1, TimeUnit.MILLISECONDS);
        assertFalse(run2WithLock.isPresent());

        Optional<LeadershipLock> run3WithLock = distributedLockRunner.runWithLock(thirdLockable, 1, TimeUnit.MINUTES);
        assertTrue(run3WithLock.isPresent());

        latch.await(2, TimeUnit.MINUTES);

        assertEquals(newArrayList("lockable1", "lockable3"), lockList.get());
    }

    private static class SimpleTestLockable implements Lockable {
        private final String name;
        private final CountDownLatch latch;
        private final long sleepMillis;

        SimpleTestLockable(String name, CountDownLatch latch, long sleepMillis) {
            this.name = name;
            this.latch = latch;
            this.sleepMillis = sleepMillis;
        }

        @Override
        public void takeLeadership(Leadership leadership) throws Exception {
            Thread.sleep(sleepMillis);
            List<String> newLockList = lockList.get();
            newLockList.add(name);

            lockList.set(newLockList);
            latch.countDown();
        }
    }
}

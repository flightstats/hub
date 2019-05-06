package com.flightstats.hub.cluster;

import com.flightstats.hub.test.Integration;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static com.google.common.collect.Lists.newArrayList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@Slf4j
public class DistributedLockRunnerTest {
    private static DistributedLeaderLockManager lockManager;
    private static AtomicReference<List<String>> lockList;
    private DistributedLockRunner distributedLockRunner;

    @BeforeClass
    public static void setupCurator() throws Exception {
        final ZooKeeperState zooKeeperState = new ZooKeeperState();
        final CuratorFramework curator = Integration.startZooKeeper(zooKeeperState);
        lockManager = new DistributedLeaderLockManager(curator, zooKeeperState);
    }

    @Before
    public void setup() {
        lockList = new AtomicReference<>(Collections.synchronizedList(new ArrayList<>()));
        this.distributedLockRunner = new DistributedLockRunner(lockManager);
    }

    @Test
    public void test_runWithLock_executesTheRunnableCode() throws Exception {
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
    public void test_runWithLock_withTwoLockables_runsOneAtATime() throws Exception {

        final CountDownLatch waitForMe = new CountDownLatch(1);
        final CountDownLatch latch = new CountDownLatch(2);
        final ExecutorService executorService = Executors.newFixedThreadPool(3);

        executorService.execute(() -> {
            Consumer<LeadershipLock> lockable = leadershipLock -> {
                waitForMe.countDown();
                doAThing(100, "lockable1", latch);
            };
            distributedLockRunner.runWithLock(lockable, "/LockPath", 1, TimeUnit.MILLISECONDS);
        });

        executorService.execute(() -> {
            Consumer<LeadershipLock> lockable = leadershipLock -> {
                doAThing(0, "lockable2", latch);
            };
            try {
                waitForMe.await(90, TimeUnit.MILLISECONDS);
            } catch (Exception e) {
            }
            distributedLockRunner.runWithLock(lockable, "/LockPath", 1, TimeUnit.MINUTES);
        });

        latch.await(5, TimeUnit.MINUTES);
        assertEquals(newArrayList("lockable1", "lockable2"), lockList.get());
    }

    @Test
    public void test_runWithLock_afterAFailureToLock_continuesLocking() throws Exception {

        final CountDownLatch waitForMe = new CountDownLatch(1);
        final CountDownLatch latch = new CountDownLatch(2);
        final ExecutorService executorService = Executors.newFixedThreadPool(3);

        executorService.execute(() -> {
            Consumer<LeadershipLock> lockable = leadershipLock -> {
                waitForMe.countDown();
                doAThing(100, "lockable1", latch);
            };
            distributedLockRunner.runWithLock(lockable, "/LockPath", 1, TimeUnit.MINUTES);
        });

        executorService.execute(() -> {
            Consumer<LeadershipLock> lockable = leadershipLock -> doAThing(0, "lockable2", latch);

            try {
                waitForMe.await(50, TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                fail(e.getMessage());
            }
            distributedLockRunner.runWithLock(lockable, "/LockPath", 1, TimeUnit.MILLISECONDS);
        });

        executorService.execute(() -> {
            Consumer<LeadershipLock> lockable = leadershipLock -> {
                doAThing(100, "lockable3", latch);
            };
            distributedLockRunner.runWithLock(lockable, "/LockPath", 1, TimeUnit.MINUTES);
        });

        latch.await(5, TimeUnit.MINUTES);
        assertEquals(2, lockList.get().size());
        assertTrue(lockList.get().contains("lockable1"));
        assertTrue(lockList.get().contains("lockable3"));
    }

    @Test
    public void test_runWithLock_withTwoSeparateLocks_isAbleToLockOnBothPaths() {

        final CountDownLatch waitForOne = new CountDownLatch(1);
        final CountDownLatch waitForTwo = new CountDownLatch(1);
        final CountDownLatch latch = new CountDownLatch(3);
        final ExecutorService executorService = Executors.newFixedThreadPool(3);
        executorService.execute(() -> {
            Consumer<LeadershipLock> lockable = leadershipLock -> {
                waitForOne.countDown();
                doAThing(200, "lockable1", latch);
                try {
                    waitForTwo.await(50, TimeUnit.MILLISECONDS);
                } catch (Exception e) {
                    fail(e.getMessage());
                }
            };

            distributedLockRunner.runWithLock(lockable, "/LockPath", 1, TimeUnit.MINUTES);
        });

        executorService.execute(() -> {
            Consumer<LeadershipLock> lockable = leadershipLock -> {
                doAThing(0, "lockable2", latch);
                waitForTwo.countDown();
            };

            try {
                waitForOne.await(50, TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                fail(e.getMessage());
            }
            distributedLockRunner.runWithLock(lockable, "/LockPath2", 1, TimeUnit.MILLISECONDS);
        });

        executorService.execute(() -> {
            Consumer<LeadershipLock> lockable = leadershipLock -> {
                doAThing(0, "lockable3", latch);
            };

            try {
                waitForOne.await(50, TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                fail(e.getMessage());
            }
            distributedLockRunner.runWithLock(lockable, "/LockPath", 1, TimeUnit.MINUTES);
        });

        try {
            latch.await(2, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            fail("something went wrong waiting for all the locks to finish!");
        }
        assertEquals("lockable2", lockList.get().get(0));
        assertTrue(lockList.get().containsAll(newArrayList("lockable1", "lockable2", "lockable3")));
    }

    private void doAThing(long sleepMillis, String name, CountDownLatch latch) {
        try {
            Thread.sleep(sleepMillis);
        } catch (Exception e) {
            fail(e.getMessage());
        }

        log.debug("Updating list {} at {}",name, DateTime.now().toString());
        lockList.getAndUpdate(list -> {
            list.add(name);
            return list;
        });

        latch.countDown();
    }
}

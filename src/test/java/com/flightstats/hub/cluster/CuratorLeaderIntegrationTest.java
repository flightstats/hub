package com.flightstats.hub.cluster;

import com.flightstats.hub.app.config.GuiceContext;
import com.flightstats.hub.test.Integration;
import com.flightstats.hub.util.Sleeper;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CuratorLeaderIntegrationTest {
    private static final Logger logger = LoggerFactory.getLogger(CuratorLeaderIntegrationTest.class);
    private static CuratorFramework curator;
    private AtomicInteger count;
    private CountDownLatch countDownLatch;

    @BeforeClass
    public static void setUpClass() throws Exception {
        Integration.startZooKeeper();
        RetryPolicy retryPolicy = GuiceContext.HubCommonModule.buildRetryPolicy();
        curator = GuiceContext.HubCommonModule.buildCurator("hub", "test", "localhost:2181", retryPolicy, new ZooKeeperState());
    }

    @Test
    public void testElection() throws Exception {
        count = new AtomicInteger();
        countDownLatch = new CountDownLatch(1);
        CuratorLeader curatorLeader = new CuratorLeader("/CuratorLeaderIntegrationTest/testElection",
                new MockElectedLeader(), curator);

        curatorLeader.start();
        assertTrue(countDownLatch.await(5000, TimeUnit.MILLISECONDS));
        assertEquals(1, count.get());
    }

    @Test
    public void testMultipleLeaders() throws Exception {
        count = new AtomicInteger();
        countDownLatch = new CountDownLatch(3);
        String leaderPath = "/CuratorLeaderIntegrationTest/testMultipleLeaders";
        CuratorLeader curatorLeader1 = new CuratorLeader(leaderPath, new MockElectedLeader(), curator);
        CuratorLeader curatorLeader2 = new CuratorLeader(leaderPath, new MockElectedLeader(), curator);
        CuratorLeader curatorLeader3 = new CuratorLeader(leaderPath, new MockElectedLeader(), curator);

        curatorLeader1.start();
        curatorLeader2.start();
        curatorLeader3.start();

        assertTrue(countDownLatch.await(5000, TimeUnit.MILLISECONDS));

        assertEquals(3, count.get());
    }

    private class MockElectedLeader implements ElectedLeader {

        @Override
        public void doWork() {
            logger.info("do Work");
            Sleeper.sleep(20);
            countDownLatch.countDown();
            count.incrementAndGet();
        }

    }

}

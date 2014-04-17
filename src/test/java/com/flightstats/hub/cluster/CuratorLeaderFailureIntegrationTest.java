package com.flightstats.hub.cluster;

import org.apache.curator.framework.CuratorFramework;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This test class doesn't seem to play well with others on Jenkins.
 */
public class CuratorLeaderFailureIntegrationTest {
    private static final Logger logger = LoggerFactory.getLogger(CuratorLeaderFailureIntegrationTest.class);
    private static CuratorFramework curator;
    private AtomicInteger count;
    private CountDownLatch countDownLatch;

    @Test
    public void testNothing() throws Exception {

    }
    /*@Before
    public void setUp() throws Exception {
        Integration.startZooKeeper();
        RetryPolicy retryPolicy = GuiceContext.HubCommonModule.buildRetryPolicy();
        curator = GuiceContext.HubCommonModule.buildCurator("hub", "test", "localhost:2181", retryPolicy, new ZooKeeperState());
    }

    @Test
    public void testLostLeaderSleep() throws Exception {
        count = new AtomicInteger();
        CuratorLeader curatorLeader = new CuratorLeader("/CuratorLeaderFailureIntegrationTest/testLostLeaderSleep",
                new LostSleepLeader(), curator);
        curatorLeader.start();
        Sleeper.sleep(50);
        Integration.stopZooKeeper();
        logger.info("count " + count.get());
        assertTrue(count.get() >= 3);
    }

    private class LostSleepLeader implements Leader {

        @Override
        public void takeLeadership(AtomicBoolean hasLeadership) {
            while (hasLeadership.get()) {
                count.incrementAndGet();
                Sleeper.sleep(5);
            }
        }
    }

    @Test
    public void testLostLeaderConnection() throws Exception {
        count = new AtomicInteger();
        countDownLatch = new CountDownLatch(1);
        CuratorLeader curatorLeader = new CuratorLeader("/CuratorLeaderFailureIntegrationTest/testLostLeaderConnection",
                new LostConnectionLeader(), curator);
        curatorLeader.start();
        Sleeper.sleep(500);
        Integration.stopZooKeeper();
        logger.info("count " + count.get());
        assertTrue(count.get() >= 1);
        assertTrue(countDownLatch.await(5000, TimeUnit.MILLISECONDS));
        curatorLeader.close();
    }

    private class LostConnectionLeader implements Leader {

        @Override
        public void takeLeadership(AtomicBoolean hasLeadership) {
            logger.info("starting work!");
            try {
                Client client = GuiceContext.HubCommonModule.buildJerseyClient();
                while (hasLeadership.get()) {
                    count.incrementAndGet();
                    logger.info("calling google " + count.get());
                    ClientResponse response = client.resource("http://www.google.com/").get(ClientResponse.class);
                    logger.info("got response " + response);
                    assertEquals(200, response.getStatus());
                }
            } catch (Exception e) {
                logger.info("caught exception ", e);
                throw new RuntimeException(e);
            }
            countDownLatch.countDown();
        }

    }
*/
}

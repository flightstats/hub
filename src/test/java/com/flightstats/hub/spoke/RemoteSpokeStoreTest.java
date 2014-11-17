package com.flightstats.hub.spoke;

import org.apache.commons.lang3.StringUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;
import static spark.Spark.put;
import static spark.SparkBase.setPort;

public class RemoteSpokeStoreTest {

    private final static Logger logger = LoggerFactory.getLogger(RemoteSpokeStoreTest.class);
    private static final byte[] PAYLOAD = "data".getBytes();

    @BeforeClass
    public static void setUpClass() throws Exception {
        setPort(4567);
    }

    @Test
    public void testWriteOneServer() throws Exception {
        String name = "testWriteOneServer";
        SpokeCluster cluster = new SpokeCluster("localhost:4567/" + name);
        CountDownLatch latch = new CountDownLatch(1);
        put("/" + name + "/spoke/payload/*", (req, res) -> {
            res.status(201);
            latch.countDown();
            return "created";
        });
        RemoteSpokeStore spokeStore = new RemoteSpokeStore(cluster);
        assertTrue(spokeStore.write("A/B", PAYLOAD));
        assertTrue(latch.await(1, TimeUnit.SECONDS));
    }

    @Test
    public void testWriteTwoServers() throws Exception {
        String name = "testWriteTwoServers";
        SpokeCluster cluster = new SpokeCluster(StringUtils.repeat("localhost:4567/" + name, ",", 2));
        CountDownLatch latch = new CountDownLatch(2);
        put("/" + name + "/spoke/payload/*", (req, res) -> {
            res.status(201);
            latch.countDown();
            return "created";
        });
        RemoteSpokeStore spokeStore = new RemoteSpokeStore(cluster);
        assertTrue(spokeStore.write("A/B", PAYLOAD));
        assertTrue(latch.await(1, TimeUnit.SECONDS));
    }

    @Test
    public void testWriteThreeServers() throws Exception {
        String name = "testWriteThreeServers";
        SpokeCluster cluster = new SpokeCluster(StringUtils.repeat("localhost:4567/" + name, ",", 3));
        CountDownLatch latch = new CountDownLatch(3);
        put("/" + name + "/spoke/payload/*", (req, res) -> {
            res.status(201);
            latch.countDown();
            return "created";
        });
        RemoteSpokeStore spokeStore = new RemoteSpokeStore(cluster);
        assertTrue(spokeStore.write("A/B", PAYLOAD));
        assertTrue(latch.await(1, TimeUnit.SECONDS));
    }

    @Test
    public void testWriteThreeServersOne500() throws Exception {
        String name = "testWriteThreeServersOne500";
        SpokeCluster cluster = new SpokeCluster("localhost:4567/" + name + ",localhost:4567/500,localhost:4567/" + name);
        CountDownLatch success = new CountDownLatch(2);
        CountDownLatch fail = new CountDownLatch(1);
        put("/" + name + "/spoke/payload/*", (req, res) -> {
            res.status(201);
            success.countDown();
            return "created";
        });
        put("/500/spoke/payload/*", (req, res) -> {
            res.status(500);
            fail.countDown();
            return "fail";
        });
        RemoteSpokeStore spokeStore = new RemoteSpokeStore(cluster);
        assertTrue(spokeStore.write("A/B", PAYLOAD));
        assertTrue(success.await(1, TimeUnit.SECONDS));
        assertTrue(fail.await(1, TimeUnit.SECONDS));
    }

    @Test
    public void testWriteThreeServersOneMissing() throws Exception {
        String name = "testWriteThreeServersOneMissing";
        SpokeCluster cluster = new SpokeCluster("localhost:4567/" + name + ",localhost:9876/missing,localhost:4567/" + name);
        CountDownLatch success = new CountDownLatch(2);
        put("/" + name + "/spoke/payload/*", (req, res) -> {
            res.status(201);
            success.countDown();
            return "created";
        });
        RemoteSpokeStore spokeStore = new RemoteSpokeStore(cluster);
        assertTrue(spokeStore.write("A/B", PAYLOAD));
        assertTrue(success.await(1, TimeUnit.SECONDS));
    }

    //todo - gfm - 11/17/14 - how do we re-create the rolling restart 60s wait time for posts?
    /**
     * request comes into server A
     * server B is currently being restarted
     * server C is up
     */

}




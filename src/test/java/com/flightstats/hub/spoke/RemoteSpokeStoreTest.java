package com.flightstats.hub.spoke;

import com.flightstats.hub.model.ContentKey;
import org.apache.commons.lang3.StringUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static spark.Spark.get;
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

    @Test
    public void testWriteThreeDifferentServers() throws Exception {

        SpokeCluster cluster = new SpokeCluster("localhost:4567/serverOne,localhost:4567/serverTwo,localhost:4567/serverThree");
        CountDownLatch one = new CountDownLatch(1);
        CountDownLatch two = new CountDownLatch(1);
        CountDownLatch three = new CountDownLatch(1);
        put("/serverOne/spoke/payload/*", (req, res) -> {
            res.status(201);
            one.countDown();
            return "created";
        });
        put("/serverTwo/spoke/payload/*", (req, res) -> {
            res.status(201);
            two.countDown();
            return "created";
        });
        put("/serverThree/spoke/payload/*", (req, res) -> {
            res.status(201);
            three.countDown();
            return "created";
        });
        RemoteSpokeStore spokeStore = new RemoteSpokeStore(cluster);
        assertTrue(spokeStore.write("A/B", PAYLOAD));
        assertTrue(one.await(1, TimeUnit.SECONDS));
        assertTrue(two.await(1, TimeUnit.SECONDS));
        assertTrue(three.await(1, TimeUnit.SECONDS));
    }

    @Test
    public void testTimeBucketUsing3Servers() throws Exception{
        SpokeCluster cluster = new SpokeCluster("localhost:4567/serverOne,localhost:4567/serverTwo,localhost:4567/serverThree");
        String one = "a/b/10.txt";
        String two = "a/b/11.txt";
        String three = "a/b/12.txt";
        String allThree = one + "," + two + "," + three;
        String just2 = one + "," + two;
        get("/serverOne/spoke/payload/*", (req, res) -> {
            res.status(200);
            return allThree;
        });
        get("/serverTwo/spoke/payload/*", (req, res) -> {
            res.status(200);
            return allThree;
        });
        get("/serverThree/spoke/payload/*", (req, res) -> {
            res.status(200);
            return just2;
        });
        RemoteSpokeStore spokeStore = new RemoteSpokeStore(cluster);
        Collection<String> keys = spokeStore.readTimeBucket("a/b");
        assertEquals(keys.size(), 3);
    }

    @Test
    public void testNextUsing3Servers() throws Exception {
        SpokeCluster cluster = new SpokeCluster("localhost:4567/serverOne,localhost:4567/serverTwo,localhost:4567/serverThree");
        String one = "a/b/10.txt";
        String two = "a/b/11.txt";
        String three = "a/b/12.txt";
        String allThree = one + "," + two + "," + three;
        String just2 = one + "," + two;

        String nextPath = "/spoke/next/*";
        get("/serverOne" + nextPath, (req, res) -> {
            res.status(200);
            return two;
        });
        get("/serverTwo" + nextPath, (req, res) -> {
            res.status(200);
            return two;
        });
        get("/serverThree" + nextPath, (req, res) -> {
            res.status(200);
            return three;
        });
        RemoteSpokeStore spokeStore = new RemoteSpokeStore(cluster);
        String key = spokeStore.readNext(one);
        assertEquals(two, key);
    }
}




package com.flightstats.hub.spoke;

import com.flightstats.hub.metrics.HostedGraphiteSender;
import com.flightstats.hub.model.Content;

public class RemoteSpokeStoreTest {

    private static final byte[] PAYLOAD = "data".getBytes();
    private Content content;
    private HostedGraphiteSender sender;

   /* @BeforeClass
    public static void setUpClass() throws Exception {
        setPort(4567);
    }

    @Before
    public void setUp() throws Exception {
        content = Content.builder().withContentKey(new ContentKey()).build();
        sender = new HostedGraphiteSender(false, "", 0, "");
    }

    @Test
    public void testWriteOneServer() throws Exception {
        String name = "testWriteOneServer";
        SpokeCluster cluster = new StringSpokeCluster("localhost:4567/" + name);
        CountDownLatch latch = new CountDownLatch(1);
        put("/" + name + "/spoke/payload*//*", new Route() {
            @Override
            public Object handle(Request req, Response res) {
                res.status(201);
                latch.countDown();
                return "created";
            }
        });
        RemoteSpokeStore spokeStore = new RemoteSpokeStore(cluster, sender);
        assertTrue(spokeStore.write("A/B", PAYLOAD, content));
        assertTrue(latch.await(1, TimeUnit.SECONDS));
    }

    @Test
    public void testWriteTwoServers() throws Exception {
        String name = "testWriteTwoServers";
        SpokeCluster cluster = new StringSpokeCluster(StringUtils.repeat("localhost:4567/" + name, ",", 2));
        CountDownLatch latch = new CountDownLatch(2);
        put("/" + name + "/spoke/payload*//*", new Route() {
            @Override
            public Object handle(Request req, Response res) {
                res.status(201);
                latch.countDown();
                return "created";
            }
        });
        RemoteSpokeStore spokeStore = new RemoteSpokeStore(cluster, sender);
        assertTrue(spokeStore.write("A/B", PAYLOAD, content));
        assertTrue(latch.await(1, TimeUnit.SECONDS));
    }

    @Test
    public void testWriteThreeServers() throws Exception {
        String name = "testWriteThreeServers";
        SpokeCluster cluster = new StringSpokeCluster(StringUtils.repeat("localhost:4567/" + name, ",", 3));
        CountDownLatch latch = new CountDownLatch(3);
        put("/" + name + "/spoke/payload*//*", new Route() {
            @Override
            public Object handle(Request req, Response res) {
                res.status(201);
                latch.countDown();
                return "created";
            }
        });
        RemoteSpokeStore spokeStore = new RemoteSpokeStore(cluster, sender);
        assertTrue(spokeStore.write("A/B", PAYLOAD, content));
        assertTrue(latch.await(1, TimeUnit.SECONDS));
    }

    @Test
    public void testWriteThreeServersOne500() throws Exception {
        String name = "testWriteThreeServersOne500";
        SpokeCluster cluster = new StringSpokeCluster("localhost:4567/" + name + ",localhost:4567/500,localhost:4567/" + name);
        CountDownLatch success = new CountDownLatch(2);
        CountDownLatch fail = new CountDownLatch(1);
        put("/" + name + "/spoke/payload*//*", new Route() {
            @Override
            public Object handle(Request req, Response res) {
                res.status(201);
                success.countDown();
                return "created";
            }
        });
        put("/500/spoke/payload*//*", new Route() {
            @Override
            public Object handle(Request req, Response res) {
                res.status(500);
                fail.countDown();
                return "fail";
            }
        });
        RemoteSpokeStore spokeStore = new RemoteSpokeStore(cluster, sender);
        assertTrue(spokeStore.write("A/B", PAYLOAD, content));
        assertTrue(success.await(1, TimeUnit.SECONDS));
        assertTrue(fail.await(1, TimeUnit.SECONDS));
    }

    @Test
    public void testWriteThreeServersOneMissing() throws Exception {
        String name = "testWriteThreeServersOneMissing";
        SpokeCluster cluster = new StringSpokeCluster("localhost:4567/" + name + ",localhost:9876/missing,localhost:4567/" + name);
        CountDownLatch success = new CountDownLatch(2);
        put("/" + name + "/spoke/payload*//*", new Route() {
            @Override
            public Object handle(Request req, Response res) {
                res.status(201);
                success.countDown();
                return "created";
            }
        });
        RemoteSpokeStore spokeStore = new RemoteSpokeStore(cluster, sender);
        assertTrue(spokeStore.write("A/B", PAYLOAD, content));
        assertTrue(success.await(1, TimeUnit.SECONDS));
    }

    @Test
    public void testWriteThreeDifferentServers() throws Exception {

        SpokeCluster cluster = new StringSpokeCluster("localhost:4567/serverOne,localhost:4567/serverTwo,localhost:4567/serverThree");
        CountDownLatch one = new CountDownLatch(1);
        CountDownLatch two = new CountDownLatch(1);
        CountDownLatch three = new CountDownLatch(1);
        put("/serverOne/spoke/payload*//*", new Route() {
            @Override
            public Object handle(Request req, Response res) {
                res.status(201);
                one.countDown();
                return "created";
            }
        });
        put("/serverTwo/spoke/payload*//*", new Route() {
            @Override
            public Object handle(Request req, Response res) {
                res.status(201);
                two.countDown();
                return "created";
            }
        });
        put("/serverThree/spoke/payload*//*", new Route() {
            @Override
            public Object handle(Request req, Response res) {
                res.status(201);
                three.countDown();
                return "created";
            }
        });
        RemoteSpokeStore spokeStore = new RemoteSpokeStore(cluster, sender);
        assertTrue(spokeStore.write("A/B", PAYLOAD, content));
        assertTrue(one.await(1, TimeUnit.SECONDS));
        assertTrue(two.await(1, TimeUnit.SECONDS));
        assertTrue(three.await(1, TimeUnit.SECONDS));
    }

    @Test
    public void testTimeBucketUsing3Servers() throws Exception {
        SpokeCluster cluster = new StringSpokeCluster("localhost:4567/serverOne,localhost:4567/serverTwo,localhost:4567/serverThree");
        DateTime now = TimeUtil.now();
        String channel = "channy";
        String one = channel + "/" + new ContentKey(now, "A").toUrl();
        String two = channel + "/" + new ContentKey(now, "B").toUrl();
        String three = channel + "/" + new ContentKey(now, "C").toUrl();

        String allThree = one + "," + two + "," + three;
        String just2 = one + "," + two;
        get("/serverOne/spoke/time*//*", new Route() {
            @Override
            public Object handle(Request req, Response res) {
                res.status(200);
                return allThree;
            }
        });
        get("/serverTwo/spoke/time*//*", new Route() {
            @Override
            public Object handle(Request req, Response res) {
                res.status(200);
                return allThree;
            }
        });
        get("/serverThree/spoke/time*//*", new Route() {
            @Override
            public Object handle(Request req, Response res) {
                res.status(200);
                return just2;
            }
        });
        RemoteSpokeStore spokeStore = new RemoteSpokeStore(cluster, sender);

        Collection<ContentKey> keys = spokeStore.readTimeBucket(channel, TimeUtil.hours(now));
        assertEquals(3, keys.size());
    }
*/
   /*
    todo - gfm - 11/19/14 - get this working too
   @Test
    public void testNextUsing3Servers() throws Exception {
        SpokeCluster cluster = new StringSpokeCluster("localhost:4567/serverOne,localhost:4567/serverTwo,localhost:4567/serverThree");
        String one = "a/b/10.txt";
        String two = "a/b/11.txt";
        String three = "a/b/12.txt";

        String nextPath = "/spoke/next*//*";
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
        RemoteSpokeStore spokeStore = new RemoteSpokeStore(cluster, sender);
        String key = spokeStore.readNext(one);
        assertEquals(two, key);
    }*/
}




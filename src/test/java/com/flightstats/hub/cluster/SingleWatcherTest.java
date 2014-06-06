package com.flightstats.hub.cluster;

import com.flightstats.hub.test.Integration;
import com.google.common.primitives.Longs;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.CuratorEvent;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;

public class SingleWatcherTest {
    private final String PATH = "/SingleWatcherTest/simple";

    @Test
    public void testSimple() throws Exception {
        CuratorFramework curator = Integration.startZooKeeper();
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final CountDownLatch countDownLatch2 = new CountDownLatch(2);
        curator.create().creatingParentsIfNeeded().forPath(PATH, Longs.toByteArray(System.currentTimeMillis()));
        SingleWatcher watcher = new SingleWatcher(curator);
        watcher.register(new Watcher() {
            @Override
            public void callback(CuratorEvent event) {
                countDownLatch.countDown();
                countDownLatch2.countDown();
            }

            @Override
            public String getPath() {
                return PATH;
            }
        });

        curator.setData().forPath(PATH, Longs.toByteArray(System.currentTimeMillis()));

        assertTrue(countDownLatch.await(1, TimeUnit.SECONDS));

        curator.setData().forPath(PATH, Longs.toByteArray(System.currentTimeMillis()));
        assertTrue(countDownLatch2.await(1, TimeUnit.SECONDS));
    }

}

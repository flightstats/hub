package com.flightstats.hub.cluster;

import com.flightstats.hub.config.PropertiesLoader;
import com.flightstats.hub.config.ZooKeeperProperties;
import com.flightstats.hub.test.Integration;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.CuratorEvent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WatchManagerTest {

    private static WatchManager watchManager;

    @BeforeAll
    static void setUpClass() throws Exception {
        CuratorFramework curator = Integration.startZooKeeper();
        watchManager = new WatchManager(curator,
                new ZooKeeperProperties(PropertiesLoader.getInstance()));
        watchManager.addCuratorListener();
    }

    @Test
    void testCallback() throws Exception {
        final CountDownLatch countDownLatch = new CountDownLatch(1);

        Watcher watcher = new Watcher() {
            @Override
            public void callback(CuratorEvent event) {
                countDownLatch.countDown();
            }

            @Override
            public String getPath() {
                return "/testCallback";
            }
        };
        watchManager.register(watcher);

        watchManager.notifyWatcher(watcher.getPath());
        assertTrue(countDownLatch.await(1, TimeUnit.SECONDS));
    }

    @Test
    void testNoCallback() throws Exception {
        final CountDownLatch countDownLatch = new CountDownLatch(1);

        Watcher watcher = new Watcher() {
            @Override
            public void callback(CuratorEvent event) {
                countDownLatch.countDown();
            }

            @Override
            public String getPath() {
                return "/testNoCallback";
            }
        };
        watchManager.register(watcher);

        watchManager.notifyWatcher("/testStuff");
        watchManager.notifyWatcher("/testNoCallbac");
        watchManager.notifyWatcher("/testNoCallback1");
        assertFalse(countDownLatch.await(1, TimeUnit.SECONDS));
    }

}
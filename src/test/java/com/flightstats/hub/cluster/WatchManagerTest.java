package com.flightstats.hub.cluster;

import com.flightstats.hub.test.TestMain;
import com.google.inject.Injector;
import org.apache.curator.framework.api.CuratorEvent;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class WatchManagerTest {

    private static WatchManager watchManager;

    @BeforeClass
    public static void setUpClass() throws Exception {
//        CuratorFramework curator = TestApplication.startZooKeeper();
//        watchManager = new WatchManager(curator);
//        watchManager.addCuratorListener();
        Injector injector = TestMain.start();
    }

    @Test
    public void testCallback() throws Exception {
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
    public void testNoCallback() throws Exception {
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
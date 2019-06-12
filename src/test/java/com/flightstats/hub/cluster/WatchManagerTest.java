package com.flightstats.hub.cluster;

import com.flightstats.hub.config.properties.ZooKeeperProperties;
import com.flightstats.hub.test.IntegrationTestSetup;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.CuratorEvent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class WatchManagerTest {

    private WatchManager watchManager;
    private CuratorFramework curator;
    @Mock
    private ZooKeeperProperties zooKeeperProperties;

    @BeforeAll
    void setupServer() {
        curator = IntegrationTestSetup.run().getZookeeperClient();
    }

    @BeforeEach
    void setup(){
        when(zooKeeperProperties.getWatchManagerThreadCount()).thenReturn(10);

        watchManager = new WatchManager(curator, zooKeeperProperties);
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
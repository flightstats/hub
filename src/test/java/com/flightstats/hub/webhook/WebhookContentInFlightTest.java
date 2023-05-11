package com.flightstats.hub.webhook;

import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.ContentPath;
import com.flightstats.hub.test.IntegrationTestSetup;
import com.flightstats.hub.util.SafeZooKeeperUtils;
import org.apache.curator.framework.CuratorFramework;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebhookContentInFlightTest {
    private SafeZooKeeperUtils zooKeeperUtils;
    private WebhookContentInFlight keysInFlight;
    private String groupName;

    @BeforeEach
    void setUp() {
        CuratorFramework curator = IntegrationTestSetup.run().getZookeeperClient();
        zooKeeperUtils = new SafeZooKeeperUtils(curator);
    }

    @Test
    void testLifecycle() throws Exception {
        keysInFlight = new WebhookContentInFlight(zooKeeperUtils);
        ContentKey first = new ContentKey();
        ContentKey second = new ContentKey();
        ContentKey third = new ContentKey();
        groupName = "testLifecycle";
        addAndCompare(first, 1);
        addAndCompare(second, 2);
        addAndCompare(third, 3);
        removeAndCompare(second, 2);
        removeAndCompare(first, 1);
        removeAndCompare(third, 0);
    }

    private void removeAndCompare(ContentKey key, int expected) {
        keysInFlight.remove(groupName, key);
        Set<ContentPath> set = keysInFlight.getSet(groupName, key);
        assertEquals(expected, set.size());
        assertFalse(set.contains(key));
    }

    private void addAndCompare(ContentKey key, int expected) {
        keysInFlight.add(groupName, key);
        Set<ContentPath> set = keysInFlight.getSet(groupName, key);
        assertEquals(expected, set.size());
        assertTrue(set.contains(key));
    }

    @Test
    void testDelete() throws Exception {
        keysInFlight = new WebhookContentInFlight(zooKeeperUtils);
        groupName = "testDelete";
        ContentKey contentKey = new ContentKey();
        addAndCompare(contentKey, 1);
        addAndCompare(new ContentKey(), 2);
        addAndCompare(new ContentKey(), 3);
        keysInFlight.delete(groupName);
        assertEquals(0, keysInFlight.getSet(groupName, contentKey).size());

    }

}
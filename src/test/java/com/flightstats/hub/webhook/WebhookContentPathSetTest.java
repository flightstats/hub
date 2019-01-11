package com.flightstats.hub.webhook;

import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.ContentPath;
import com.flightstats.hub.test.Integration;
import com.flightstats.hub.util.SafeZooKeeperUtils;
import org.apache.curator.framework.CuratorFramework;
import org.junit.Before;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.*;

public class WebhookContentPathSetTest {
    private SafeZooKeeperUtils zooKeeperUtils;
    private WebhookContentPathSet groupSet;
    private String groupName;

    @Before
    public void setUp() throws Exception {
        CuratorFramework curator = Integration.startZooKeeper();
        zooKeeperUtils = new SafeZooKeeperUtils(curator);
    }

    @Test
    public void testLifecycle() throws Exception {
        groupSet = new WebhookContentPathSet(zooKeeperUtils);
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
        groupSet.remove(groupName, key);
        Set<ContentPath> set = groupSet.getSet(groupName, key);
        assertEquals(expected, set.size());
        assertFalse(set.contains(key));
    }

    private void addAndCompare(ContentKey key, int expected) {
        groupSet.add(groupName, key);
        Set<ContentPath> set = groupSet.getSet(groupName, key);
        assertEquals(expected, set.size());
        assertTrue(set.contains(key));
    }

    @Test
    public void testDelete() throws Exception {
        groupSet = new WebhookContentPathSet(zooKeeperUtils);
        groupName = "testDelete";
        ContentKey contentKey = new ContentKey();
        addAndCompare(contentKey, 1);
        addAndCompare(new ContentKey(), 2);
        addAndCompare(new ContentKey(), 3);
        groupSet.delete(groupName);
        assertEquals(0, groupSet.getSet(groupName, contentKey).size());

    }

}
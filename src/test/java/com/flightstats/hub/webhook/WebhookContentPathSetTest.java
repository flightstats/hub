package com.flightstats.hub.webhook;

import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.ContentPath;
import com.flightstats.hub.test.TestMain;
import com.google.inject.Injector;
import org.apache.curator.framework.CuratorFramework;
import org.junit.Before;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.*;

public class WebhookContentPathSetTest {

    private CuratorFramework curator;
    private WebhookContentPathSet groupSet;
    private String groupName;

    @Before
    public void setUp() throws Exception {
//        curator = TestApplication.startZooKeeper();
        Injector injector = TestMain.start();
    }

    @Test
    public void testLifecycle() throws Exception {
        groupSet = new WebhookContentPathSet(curator);
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
        groupSet = new WebhookContentPathSet(curator);
        groupName = "testDelete";
        ContentKey contentKey = new ContentKey();
        addAndCompare(contentKey, 1);
        addAndCompare(new ContentKey(), 2);
        addAndCompare(new ContentKey(), 3);
        groupSet.delete(groupName);
        assertEquals(0, groupSet.getSet(groupName, contentKey).size());

    }

}
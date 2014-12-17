package com.flightstats.hub.group;

import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.test.Integration;
import org.apache.curator.framework.CuratorFramework;
import org.junit.Before;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class GroupContentKeySetTest {

    private CuratorFramework curator;
    private GroupContentKeySet groupSet;
    private String groupName;

    @Before
    public void setUp() throws Exception {
        curator = Integration.startZooKeeper();
    }

    @Test
    public void testLifecycle() throws Exception {
        groupSet = new GroupContentKeySet(curator);
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
        Set<ContentKey> set = groupSet.getSet(groupName);
        assertEquals(expected, set.size());
        assertFalse(set.contains(key));
    }

    private void addAndCompare(ContentKey key, int expected) {
        groupSet.add(groupName, key);
        Set<ContentKey> set = groupSet.getSet(groupName);
        assertEquals(expected, set.size());
        assertTrue(set.contains(key));
    }

    @Test
    public void testDelete() throws Exception {
        groupSet = new GroupContentKeySet(curator);
        groupName = "testDelete";
        addAndCompare(new ContentKey(), 1);
        addAndCompare(new ContentKey(), 2);
        addAndCompare(new ContentKey(), 3);
        groupSet.delete(groupName);
        assertEquals(0, groupSet.getSet(groupName).size());

    }

}
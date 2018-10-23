package com.flightstats.hub.cluster;

import com.flightstats.hub.test.TestMain;
import com.google.common.collect.Sets;
import com.google.inject.Injector;
import org.apache.curator.framework.CuratorFramework;
import org.junit.Before;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class LongSetTest {

    private CuratorFramework curator;
    private LongSet longSet;

    @Before
    public void setUp() throws Exception {
//        curator = TestApplication.startZooKeeper();
        Injector injector = TestMain.start();
    }

    @Test
    public void testLifecycle() throws Exception {
        String path = "/test/longs/lifecycle";
        longSet = new LongSet(path, curator);
        Set<Long> firstSet = Sets.newHashSet(100L, 101L, 102L);
        addItems(firstSet);
        assertEquals(3, longSet.getSet().size());
        assertTrue(longSet.getSet().containsAll(firstSet));
        Set<Long> secondSet = Sets.newHashSet(200L, 201L, 202L);
        addItems(secondSet);
        assertEquals(6, longSet.getSet().size());
        assertTrue(longSet.getSet().containsAll(firstSet));
        assertTrue(longSet.getSet().containsAll(secondSet));
        removeItems(firstSet);
        assertEquals(3, longSet.getSet().size());
        assertTrue(longSet.getSet().containsAll(secondSet));
        LongSet.delete(path, curator);
        assertEquals(0, longSet.getSet().size());
    }

    private void removeItems(Set<Long> set) {
        for (Long value : set) {
            longSet.remove(value);
        }
    }

    private void addItems(Set<Long> set) {
        for (Long value : set) {
            longSet.add(value);
        }
    }
}
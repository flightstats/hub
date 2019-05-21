package com.flightstats.hub.cluster;

import com.flightstats.hub.test.IntegrationTestSetup;
import com.google.common.collect.Sets;
import org.apache.curator.framework.CuratorFramework;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LongSetTest {

    private CuratorFramework curator;
    private LongSet longSet;

    @BeforeEach
    void setUp() {
        curator = IntegrationTestSetup.run().getZookeeperClient();
    }

    @Test
    void testLifecycle() throws Exception {
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
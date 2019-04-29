package com.flightstats.hub.cluster;

import com.flightstats.hub.test.Integration;
import org.apache.curator.framework.CuratorFramework;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LongValueTest {

    private static CuratorFramework curator;
    private LongValue longValue;

    @BeforeAll
    static void setUpClass() throws Exception {
        curator = Integration.startZooKeeper();
    }

    @BeforeEach
    void setUp() throws Exception {
        longValue = new LongValue(curator);
    }

    @Test
    void testLifeCycle() throws Exception {
        String path = "/LVT/testLifeCycle";
        longValue.initialize(path, 10);
        assertEquals(10, longValue.get(path, 0));
        longValue.updateIncrease(20, path);
        assertEquals(20, longValue.get(path, 0));
        longValue.delete(path);
        assertEquals(0, longValue.get(path, 0));
    }

    @Test
    void testCreateIfMissing() throws Exception {
        String path = "/LVT/testCreateIfMissing";
        assertEquals(10, longValue.get(path, 10));
        assertEquals(10, longValue.get(path, 20));
    }

    @Test
    void testUpdateIncrease() throws Exception {
        String path = "/LVT/testUpdateIncrease";
        longValue.initialize(path, 10);
        longValue.updateIncrease(5, path);
        assertEquals(10, longValue.get(path, 0));
        longValue.updateIncrease(15, path);
        assertEquals(15, longValue.get(path, 0));
    }


}
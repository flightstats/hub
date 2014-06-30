package com.flightstats.hub.cluster;

import com.flightstats.hub.test.Integration;
import org.apache.curator.framework.CuratorFramework;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class LongValueTest {

    private static CuratorFramework curator;
    private LongValue longValue;

    @BeforeClass
    public static void setUpClass() throws Exception {
        curator = Integration.startZooKeeper();
    }

    @Before
    public void setUp() throws Exception {
        longValue = new LongValue(curator);
    }

    @Test
    public void testLifeCycle() throws Exception {
        String path = "/LVT/testLifeCycle";
        longValue.initialize(path, 10);
        assertEquals(10, longValue.get(path, 0));
        longValue.update(20, path);
        assertEquals(20, longValue.get(path, 0));
        longValue.delete(path);
        assertEquals(0, longValue.get(path, 0));
    }

    @Test
    public void testCreateIfMissing() throws Exception {
        String path = "/LVT/testCreateIfMissing";
        assertEquals(10, longValue.get(path, 10));
        assertEquals(10, longValue.get(path, 20));
    }

    @Test
    public void testUpdateIncrease() throws Exception {
        String path = "/LVT/testUpdateIncrease";
        longValue.initialize(path, 10);
        longValue.updateIncrease(5, path);
        assertEquals(10, longValue.get(path, 0));
        longValue.updateIncrease(15, path);
        assertEquals(15, longValue.get(path, 0));
    }


}
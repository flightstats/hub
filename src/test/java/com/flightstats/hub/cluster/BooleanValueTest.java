package com.flightstats.hub.cluster;

import com.flightstats.hub.test.Integration;
import org.apache.curator.framework.CuratorFramework;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

public class BooleanValueTest {

    private static CuratorFramework curator;
    private BooleanValue booleanValue;

    @BeforeClass
    public static void setUpClass() throws Exception {
        curator = Integration.startZooKeeper();
    }

    @Before
    public void setUp() throws Exception {
        booleanValue = new BooleanValue(curator);
    }

    @Test
    public void testLifeCycle() throws Exception {
        String path = "/BVT/testLifeCycle";
        booleanValue.initialize(path, false);
        assertEquals(false, booleanValue.get(path, true));
        boolean set = booleanValue.setIfNotValue(path, true);
        assertEquals(true, set);
        assertEquals(true, booleanValue.get(path, false));
        booleanValue.delete(path);
        assertEquals(false, booleanValue.get(path, false));
    }

    @Test
    public void testCreateIfMissing() throws Exception {
        String path = "/BVT/testCreateIfMissing";
        assertEquals(true, booleanValue.get(path, true));
        assertEquals(true, booleanValue.get(path, false));
    }

    @Test
    public void testUpdateIncrease() throws Exception {
        String path = "/LVT/testsetIfNotValue";
        booleanValue.initialize(path, false);
        assertFalse(booleanValue.setIfNotValue(path, false));
        assertTrue(booleanValue.setIfNotValue(path, true));
        assertFalse(booleanValue.setIfNotValue(path, true));


    }

}
package com.flightstats.hub.cluster;

import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.test.Integration;
import org.apache.curator.framework.CuratorFramework;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class LastContentPathTest {

    private static CuratorFramework curator;
    private LastContentPath contentKeyValue;

    @BeforeClass
    public static void setUpClass() throws Exception {
        curator = Integration.startZooKeeper();
    }

    @Before
    public void setUp() throws Exception {
        contentKeyValue = new LastContentPath(curator);
    }

    @Test
    public void testLifeCycle() throws Exception {
        String name = "testLifeCycle";
        DateTime start = new DateTime(2014, 12, 3, 20, 45, DateTimeZone.UTC);
        ContentKey key1 = new ContentKey(start, "B");
        contentKeyValue.initialize(name, key1, "/GroupLastCompleted/");
        assertEquals(key1, contentKeyValue.get(name, new ContentKey(), "/GroupLastCompleted/"));

        ContentKey key2 = new ContentKey(start.plusMillis(1), "C");
        contentKeyValue.updateIncrease(key2, name, "/GroupLastCompleted/");
        assertEquals(key2, contentKeyValue.get(name, new ContentKey(), "/GroupLastCompleted/"));

        ContentKey key3 = new ContentKey(start.minusMillis(1), "A");
        contentKeyValue.updateIncrease(key3, name, "/GroupLastCompleted/");
        assertEquals(key2, contentKeyValue.get(name, new ContentKey(), "/GroupLastCompleted/"));

        ContentKey key4 = new ContentKey(start.plusMinutes(1), "D");
        contentKeyValue.updateIncrease(key4, name, "/GroupLastCompleted/");
        assertEquals(key4, contentKeyValue.get(name, new ContentKey(), "/GroupLastCompleted/"));

        contentKeyValue.delete(name, "/GroupLastCompleted/");
        ContentKey contentKey = new ContentKey();
        assertEquals(contentKey, contentKeyValue.get(name, contentKey, "/GroupLastCompleted/"));
    }

    @Test
    public void testCreateIfMissing() throws Exception {
        String name = "testCreateIfMissing";
        ContentKey key = new ContentKey();
        assertEquals(key, contentKeyValue.get(name, key, "/GroupLastCompleted/"));
        assertEquals(key, contentKeyValue.get(name, new ContentKey(), "/GroupLastCompleted/"));
    }

}
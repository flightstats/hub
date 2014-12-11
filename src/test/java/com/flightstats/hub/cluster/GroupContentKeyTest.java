package com.flightstats.hub.cluster;

import com.flightstats.hub.group.GroupContentKey;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.test.Integration;
import org.apache.curator.framework.CuratorFramework;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class GroupContentKeyTest {

    private static CuratorFramework curator;
    private GroupContentKey contentKeyValue;

    @BeforeClass
    public static void setUpClass() throws Exception {
        curator = Integration.startZooKeeper();
    }

    @Before
    public void setUp() throws Exception {
        contentKeyValue = new GroupContentKey(curator);
    }

    @Test
    public void testLifeCycle() throws Exception {
        String name = "testLifeCycle";
        DateTime start = new DateTime(2014, 12, 3, 20, 45, DateTimeZone.UTC);
        ContentKey key1 = new ContentKey(start, "B");
        contentKeyValue.initialize(name, key1);
        assertEquals(key1, contentKeyValue.get(name, new ContentKey()));

        ContentKey key2 = new ContentKey(start.plusMillis(1), "C");
        contentKeyValue.updateIncrease(key2, name);
        assertEquals(key2, contentKeyValue.get(name, new ContentKey()));

        ContentKey key3 = new ContentKey(start.minusMillis(1), "A");
        contentKeyValue.updateIncrease(key3, name);
        assertEquals(key2, contentKeyValue.get(name, new ContentKey()));

        ContentKey key4 = new ContentKey(start.plusMinutes(1), "D");
        contentKeyValue.updateIncrease(key4, name);
        assertEquals(key4, contentKeyValue.get(name, new ContentKey()));

        contentKeyValue.delete(name);
        ContentKey contentKey = new ContentKey();
        assertEquals(contentKey, contentKeyValue.get(name, contentKey));
    }

    @Test
    public void testCreateIfMissing() throws Exception {
        String name = "testCreateIfMissing";
        ContentKey key = new ContentKey();
        assertEquals(key, contentKeyValue.get(name, key));
        assertEquals(key, contentKeyValue.get(name, new ContentKey()));
    }

}
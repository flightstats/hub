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

public class ContentKeyValueTest {

    private static CuratorFramework curator;
    private ContentKeyValue contentKeyValue;

    @BeforeClass
    public static void setUpClass() throws Exception {
        curator = Integration.startZooKeeper();
    }

    @Before
    public void setUp() throws Exception {
        contentKeyValue = new ContentKeyValue(curator);
    }

    @Test
    public void testLifeCycle() throws Exception {
        String path = "/SVT/testLifeCycle";
        DateTime start = new DateTime(2014, 12, 3, 20, 45, DateTimeZone.UTC);
        ContentKey key1 = new ContentKey(start, "B");
        contentKeyValue.initialize(path, key1);
        assertEquals(key1, contentKeyValue.get(path, new ContentKey()));

        ContentKey key2 = new ContentKey(start.plusMillis(1), "C");
        contentKeyValue.updateIncrease(key2, path);
        assertEquals(key2, contentKeyValue.get(path, new ContentKey()));

        ContentKey key3 = new ContentKey(start.minusMillis(1), "A");
        contentKeyValue.updateIncrease(key3, path);
        assertEquals(key2, contentKeyValue.get(path, new ContentKey()));

        ContentKey key4 = new ContentKey(start.plusMinutes(1), "D");
        contentKeyValue.updateIncrease(key4, path);
        assertEquals(key4, contentKeyValue.get(path, new ContentKey()));

        contentKeyValue.delete(path);
        ContentKey contentKey = new ContentKey();
        assertEquals(contentKey, contentKeyValue.get(path, contentKey));
    }

    @Test
    public void testCreateIfMissing() throws Exception {
        String path = "/SVT/testCreateIfMissing";
        ContentKey key = new ContentKey();
        assertEquals(key, contentKeyValue.get(path, key));
        assertEquals(key, contentKeyValue.get(path, new ContentKey()));
    }

}
package com.flightstats.hub.cluster;

import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.ContentPath;
import com.flightstats.hub.model.MinutePath;
import com.flightstats.hub.test.TestMain;
import com.google.inject.Injector;
import org.apache.curator.framework.CuratorFramework;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;

public class LastContentPathTest {

    private static final String BASE_PATH = "/GroupLastCompleted/";
    private static CuratorFramework curator;
    private LastContentPath lastContentPath;

    @BeforeClass
    public static void setUpClass() throws Exception {
        Injector injector = TestMain.start();
        curator = injector.getInstance(CuratorFramework.class);
    }

    @Before
    public void setUp() {
        HubProperties hubProperties = mock(HubProperties.class);
        lastContentPath = new LastContentPath(curator, hubProperties);
    }

    @Test
    public void testLifeCycle() throws Exception {
        String name = "testLifeCycle";
        DateTime start = new DateTime(2014, 12, 3, 20, 45, DateTimeZone.UTC);
        ContentKey key1 = new ContentKey(start, "B");
        lastContentPath.initialize(name, key1, BASE_PATH);
        assertEquals(key1, lastContentPath.get(name, new ContentKey(), BASE_PATH));

        ContentKey key2 = new ContentKey(start.plusMillis(1), "C");
        lastContentPath.updateIncrease(key2, name, BASE_PATH);
        assertEquals(key2, lastContentPath.get(name, new ContentKey(), BASE_PATH));

        ContentKey key3 = new ContentKey(start.minusMillis(1), "A");
        lastContentPath.updateIncrease(key3, name, BASE_PATH);
        assertEquals(key2, lastContentPath.get(name, new ContentKey(), BASE_PATH));

        ContentKey key4 = new ContentKey(start.plusMinutes(1), "D");
        lastContentPath.updateIncrease(key4, name, BASE_PATH);
        assertEquals(key4, lastContentPath.get(name, new ContentKey(), BASE_PATH));

        lastContentPath.delete(name, BASE_PATH);
        ContentKey contentKey = new ContentKey();
        assertEquals(contentKey, lastContentPath.get(name, contentKey, BASE_PATH));
    }

    @Test
    public void testCreateNull() throws Exception {
        String name = "testCreateNull";
        DateTime start = new DateTime(2014, 12, 3, 20, 45, DateTimeZone.UTC);
        ContentKey key1 = new ContentKey(start, "B");
        ContentPath contentPath = lastContentPath.get(name, null, BASE_PATH);
        assertNull(contentPath);
    }

    @Test
    public void testCreateIfMissing() throws Exception {
        String name = "testCreateIfMissing";
        ContentKey key = new ContentKey();
        assertEquals(key, lastContentPath.get(name, key, BASE_PATH));
        assertEquals(key, lastContentPath.get(name, new ContentKey(), BASE_PATH));
    }

    @Test
    public void testMinutePath() {
        String name = "testMinutePath";

        MinutePath minutePath = new MinutePath();
        lastContentPath.initialize(name, minutePath, BASE_PATH);
        assertEquals(minutePath, lastContentPath.get(name, new MinutePath(), BASE_PATH));

        MinutePath nextPath = new MinutePath(minutePath.getTime().plusMinutes(1));
        lastContentPath.updateIncrease(nextPath, name, BASE_PATH);
        assertEquals(nextPath, lastContentPath.get(name, new MinutePath(), BASE_PATH));

        nextPath = new MinutePath(nextPath.getTime().plusMinutes(1));
        lastContentPath.updateIncrease(nextPath, name, BASE_PATH);
        assertEquals(nextPath, lastContentPath.get(name, new MinutePath(), BASE_PATH));
    }

    @Test
    public void testUpdateDecrease() throws Exception {
        String name = "testUpdateDecrease";
        DateTime start = new DateTime(2014, 12, 3, 20, 45, DateTimeZone.UTC);

        ContentKey key1 = new ContentKey(start, "B");
        lastContentPath.initialize(name, key1, BASE_PATH);
        assertEquals(key1, lastContentPath.get(name, new ContentKey(), BASE_PATH));

        ContentKey key2 = new ContentKey(start.minusMillis(1), "C");
        lastContentPath.updateDecrease(key2, name, BASE_PATH);
        assertEquals(key2, lastContentPath.get(name, new ContentKey(), BASE_PATH));

        ContentKey key3 = new ContentKey(start.plusMillis(1), "A");
        lastContentPath.updateDecrease(key3, name, BASE_PATH);
        assertEquals(key2, lastContentPath.get(name, new ContentKey(), BASE_PATH));

        ContentKey key4 = new ContentKey(start.minusMinutes(1), "D");
        lastContentPath.updateDecrease(key4, name, BASE_PATH);
        assertEquals(key4, lastContentPath.get(name, new ContentKey(), BASE_PATH));

        lastContentPath.delete(name, BASE_PATH);
        ContentKey contentKey = new ContentKey();
        assertEquals(contentKey, lastContentPath.get(name, contentKey, BASE_PATH));
    }

}

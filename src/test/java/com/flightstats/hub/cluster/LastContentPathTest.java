package com.flightstats.hub.cluster;

import com.flightstats.hub.config.AppProperties;
import com.flightstats.hub.config.PropertiesLoader;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.ContentPath;
import com.flightstats.hub.model.MinutePath;
import com.flightstats.hub.test.Integration;
import org.apache.curator.framework.CuratorFramework;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class LastContentPathTest {

    private static final String BASE_PATH = "/GroupLastCompleted/";
    private static CuratorFramework curator;
    private LastContentPath lastContentPath;

    @BeforeAll
    static void setUpClass() throws Exception {
        curator = Integration.startZooKeeper();
    }

    @BeforeEach
    void setUp() {
        lastContentPath = new LastContentPath(curator, new AppProperties(PropertiesLoader.getInstance()));
    }

    @Test
    void testLifeCycle() {
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
    void testCreateNull() {
        String name = "testCreateNull";
        ContentPath contentPath = lastContentPath.get(name, null, BASE_PATH);
        assertNull(contentPath);
    }

    @Test
    void testCreateIfMissing() {
        String name = "testCreateIfMissing";
        ContentKey key = new ContentKey();
        assertEquals(key, lastContentPath.get(name, key, BASE_PATH));
        assertEquals(key, lastContentPath.get(name, new ContentKey(), BASE_PATH));
    }

    @Test
    void testMinutePath() {
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
    void testUpdateDecrease() {
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
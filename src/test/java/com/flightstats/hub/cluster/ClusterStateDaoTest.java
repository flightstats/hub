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

class ClusterStateDaoTest {

    private static final String BASE_PATH = "/GroupLastCompleted/";
    private static CuratorFramework curator;
    private ClusterStateDao clusterStateDao;

    @BeforeAll
    static void setUpClass() throws Exception {
        curator = Integration.startZooKeeper();
    }

    @BeforeEach
    void setUp() {
        clusterStateDao = new ClusterStateDao(curator, new AppProperties(PropertiesLoader.getInstance()));
    }

    @Test
    void testLifeCycle() {
        String name = "testLifeCycle";
        DateTime start = new DateTime(2014, 12, 3, 20, 45, DateTimeZone.UTC);
        ContentKey key1 = new ContentKey(start, "B");
        clusterStateDao.initialize(name, key1, BASE_PATH);
        assertEquals(key1, clusterStateDao.get(name, new ContentKey(), BASE_PATH));

        ContentKey key2 = new ContentKey(start.plusMillis(1), "C");
        clusterStateDao.setIfAfter(key2, name, BASE_PATH);
        assertEquals(key2, clusterStateDao.get(name, new ContentKey(), BASE_PATH));

        ContentKey key3 = new ContentKey(start.minusMillis(1), "A");
        clusterStateDao.setIfAfter(key3, name, BASE_PATH);
        assertEquals(key2, clusterStateDao.get(name, new ContentKey(), BASE_PATH));

        ContentKey key4 = new ContentKey(start.plusMinutes(1), "D");
        clusterStateDao.setIfAfter(key4, name, BASE_PATH);
        assertEquals(key4, clusterStateDao.get(name, new ContentKey(), BASE_PATH));

        clusterStateDao.delete(name, BASE_PATH);
        ContentKey contentKey = new ContentKey();
        assertEquals(contentKey, clusterStateDao.get(name, contentKey, BASE_PATH));
    }

    @Test
    void testCreateNull() {
        String name = "testCreateNull";
        ContentPath contentPath = clusterStateDao.get(name, null, BASE_PATH);
        assertNull(contentPath);
    }

    @Test
    void testCreateIfMissing() {
        String name = "testCreateIfMissing";
        ContentKey key = new ContentKey();
        assertEquals(key, clusterStateDao.get(name, key, BASE_PATH));
        assertEquals(key, clusterStateDao.get(name, new ContentKey(), BASE_PATH));
    }

    @Test
    void testMinutePath() {
        String name = "testMinutePath";

        MinutePath minutePath = new MinutePath();
        clusterStateDao.initialize(name, minutePath, BASE_PATH);
        assertEquals(minutePath, clusterStateDao.get(name, new MinutePath(), BASE_PATH));

        MinutePath nextPath = new MinutePath(minutePath.getTime().plusMinutes(1));
        clusterStateDao.setIfAfter(nextPath, name, BASE_PATH);
        assertEquals(nextPath, clusterStateDao.get(name, new MinutePath(), BASE_PATH));

        nextPath = new MinutePath(nextPath.getTime().plusMinutes(1));
        clusterStateDao.setIfAfter(nextPath, name, BASE_PATH);
        assertEquals(nextPath, clusterStateDao.get(name, new MinutePath(), BASE_PATH));
    }

    @Test
    void testSetIfBefore() {
        String name = "testSetIfBefore";
        DateTime start = new DateTime(2014, 12, 3, 20, 45, DateTimeZone.UTC);

        ContentKey key1 = new ContentKey(start, "B");
        clusterStateDao.initialize(name, key1, BASE_PATH);
        assertEquals(key1, clusterStateDao.get(name, new ContentKey(), BASE_PATH));

        ContentKey key2 = new ContentKey(start.minusMillis(1), "C");
        clusterStateDao.setIfBefore(key2, name, BASE_PATH);
        assertEquals(key2, clusterStateDao.get(name, new ContentKey(), BASE_PATH));

        ContentKey key3 = new ContentKey(start.plusMillis(1), "A");
        clusterStateDao.setIfBefore(key3, name, BASE_PATH);
        assertEquals(key2, clusterStateDao.get(name, new ContentKey(), BASE_PATH));

        ContentKey key4 = new ContentKey(start.minusMinutes(1), "D");
        clusterStateDao.setIfBefore(key4, name, BASE_PATH);
        assertEquals(key4, clusterStateDao.get(name, new ContentKey(), BASE_PATH));

        clusterStateDao.delete(name, BASE_PATH);
        ContentKey contentKey = new ContentKey();
        assertEquals(contentKey, clusterStateDao.get(name, contentKey, BASE_PATH));
    }

    @Test
    void testSetIfAfter() {
        String name = "testSetIfAfter";
        DateTime start = new DateTime(2014, 12, 3, 20, 45, DateTimeZone.UTC);

        ContentKey key1 = new ContentKey(start, "B");
        clusterStateDao.initialize(name, key1, BASE_PATH);
        assertEquals(key1, clusterStateDao.get(name, new ContentKey(), BASE_PATH));

        ContentKey key2 = new ContentKey(start.plusMillis(1), "ImSlightlyAfter");
        clusterStateDao.setIfAfter(key2, name, BASE_PATH);
        assertEquals(key2, clusterStateDao.get(name, new ContentKey(), BASE_PATH));

        ContentKey key3 = new ContentKey(start, "ImNowOneMilliOld");
        clusterStateDao.setIfAfter(key3, name, BASE_PATH);
        assertEquals(key2, clusterStateDao.get(name, new ContentKey(), BASE_PATH));
    }

}
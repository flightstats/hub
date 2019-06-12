package com.flightstats.hub.cluster;

import com.flightstats.hub.config.properties.AppProperties;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.ContentPath;
import com.flightstats.hub.model.MinutePath;
import com.flightstats.hub.test.IntegrationTestSetup;
import org.apache.curator.framework.CuratorFramework;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@ExtendWith(MockitoExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ClusterCacheDaoTest {

    private static final String BASE_PATH = "/GroupLastCompleted/";
    private static CuratorFramework curator;
    private ClusterCacheDao clusterCacheDao;
    @Mock
    private AppProperties appProperties;

    @BeforeAll
    void setUpClass() {
        curator = IntegrationTestSetup.run().getZookeeperClient();
        clusterCacheDao = new ClusterCacheDao(curator, appProperties);
    }

    @Test
    void testInitializeCreatesPath() {
        String name = "testCreatesPath";
        DateTime start = new DateTime(2014, 12, 3, 20, 45, DateTimeZone.UTC);
        ContentKey key1 = new ContentKey(start, "B");
        clusterCacheDao.initialize(name, key1, BASE_PATH);
        assertEquals(key1, clusterCacheDao.get(name, new ContentKey(), BASE_PATH));
    }

    @Test
    void testGetRecreatesDeletedPath() {
        String name = "testDeletedPath";
        DateTime start = new DateTime(2014, 12, 3, 20, 45, DateTimeZone.UTC);

        ContentKey key1 = new ContentKey(start, "B");
        clusterCacheDao.initialize(name, key1, BASE_PATH);
        assertEquals(key1, clusterCacheDao.get(name, new ContentKey(), BASE_PATH));

        clusterCacheDao.delete(name, BASE_PATH);
        ContentKey contentKey = new ContentKey();
        assertEquals(contentKey, clusterCacheDao.get(name, contentKey, BASE_PATH));
    }

    @Test
    void testCreateNull() {
        String name = "testCreateNull";
        ContentPath contentPath = clusterCacheDao.get(name, null, BASE_PATH);
        assertNull(contentPath);
    }

    @Test
    void testCreateIfMissing() {
        String name = "testCreateIfMissing";
        ContentKey key = new ContentKey();
        assertEquals(key, clusterCacheDao.get(name, key, BASE_PATH));
        assertEquals(key, clusterCacheDao.get(name, new ContentKey(), BASE_PATH));
    }

    @Test
    void testMinutePath() {
        String name = "testMinutePath";

        MinutePath minutePath = new MinutePath();
        clusterCacheDao.initialize(name, minutePath, BASE_PATH);
        assertEquals(minutePath, clusterCacheDao.get(name, new MinutePath(), BASE_PATH));

        MinutePath nextPath = new MinutePath(minutePath.getTime().plusMinutes(1));
        clusterCacheDao.setIfNewer(nextPath, name, BASE_PATH);
        assertEquals(nextPath, clusterCacheDao.get(name, new MinutePath(), BASE_PATH));

        nextPath = new MinutePath(nextPath.getTime().plusMinutes(1));
        clusterCacheDao.setIfNewer(nextPath, name, BASE_PATH);
        assertEquals(nextPath, clusterCacheDao.get(name, new MinutePath(), BASE_PATH));
    }

    @Test
    void testSetIfOlder() {
        String name = "testSetIfOlder";
        DateTime start = new DateTime(2014, 12, 3, 20, 45, DateTimeZone.UTC);

        ContentKey key1 = new ContentKey(start, "B");
        clusterCacheDao.initialize(name, key1, BASE_PATH);
        assertEquals(key1, clusterCacheDao.get(name, new ContentKey(), BASE_PATH));

        ContentKey key2 = new ContentKey(start.minusMillis(1), "C");
        clusterCacheDao.setIfOlder(key2, name, BASE_PATH);
        assertEquals(key2, clusterCacheDao.get(name, new ContentKey(), BASE_PATH));

        ContentKey key3 = new ContentKey(start.plusMillis(1), "A");
        clusterCacheDao.setIfOlder(key3, name, BASE_PATH);
        assertEquals(key2, clusterCacheDao.get(name, new ContentKey(), BASE_PATH));

        ContentKey key4 = new ContentKey(start.minusMinutes(1), "D");
        clusterCacheDao.setIfOlder(key4, name, BASE_PATH);
        assertEquals(key4, clusterCacheDao.get(name, new ContentKey(), BASE_PATH));

        clusterCacheDao.delete(name, BASE_PATH);
        ContentKey contentKey = new ContentKey();
        assertEquals(contentKey, clusterCacheDao.get(name, contentKey, BASE_PATH));
    }

    @Test
    void testSetIfNewer() {
        String name = "testSetIfNewer";
        DateTime start = new DateTime(2014, 12, 3, 20, 45, DateTimeZone.UTC);

        ContentKey key1 = new ContentKey(start, "B");
        clusterCacheDao.initialize(name, key1, BASE_PATH);
        assertEquals(key1, clusterCacheDao.get(name, new ContentKey(), BASE_PATH));

        ContentKey key2 = new ContentKey(start.plusMillis(1), "ImSlightlyNewer");
        clusterCacheDao.setIfNewer(key2, name, BASE_PATH);
        assertEquals(key2, clusterCacheDao.get(name, new ContentKey(), BASE_PATH));

        ContentKey key3 = new ContentKey(start, "ImNowOneMilliOld");
        clusterCacheDao.setIfNewer(key3, name, BASE_PATH);
        assertEquals(key2, clusterCacheDao.get(name, new ContentKey(), BASE_PATH));
    }

}
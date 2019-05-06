package com.flightstats.hub.spoke;

import com.flightstats.hub.config.PropertiesLoader;
import com.flightstats.hub.config.SpokeProperties;
import com.flightstats.hub.dao.ContentKeyUtil;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.util.TimeUtil;
import com.google.common.io.Files;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@Slf4j
public class FileSpokeStoreTest {

    private static final SpokeProperties spokeProperties = new SpokeProperties(PropertiesLoader.getInstance());
    private static final int ttlMinutes = spokeProperties.getTtlMinutes(SpokeStore.WRITE);
    public static final byte[] BYTES = new byte[]{0, 2, 3, 4, 5, 6};

    private FileSpokeStore spokeStore;
    private String tempDir;

    @Before
    public void setUp() {
        tempDir = Files.createTempDir().getPath();
        PropertiesLoader.getInstance().setProperty("spoke.write.path", tempDir);
        spokeStore = new FileSpokeStore(tempDir, ttlMinutes);
    }

    @Test
    public void testWriteRead() {
        String path = "channelWR/" + new ContentKey().toUrl();
        assertTrue(spokeStore.insert(path, BYTES));
        byte[] read = spokeStore.read(path);
        assertArrayEquals(BYTES, read);
    }

    @Test
    public void testPathTranslation() {
        String incoming = "/test_0_4274725520517677/2014/11/18/00/57/24/015/NV2cl5";
        File outputFile = spokeStore.spokeFilePathPart(incoming);
        String filePath = "/test_0_4274725520517677/2014/11/18/00/57/24015NV2cl5";
        String expectedPath = tempDir + filePath;
        assertEquals(expectedPath, outputFile.getAbsolutePath());
        final File file = new File(filePath);
        String urlPart = spokeStore.spokeKeyFromPath(file.getAbsolutePath());
        assertEquals(incoming, urlPart);
    }

    @Test
    public void testAdjacentPaths() {
        String previousSecond = "testAdjacentPaths/2014/11/18/00/57/23/015/1";
        String path1 = "testAdjacentPaths/2014/11/18/00/57/24/015/1";
        String path2 = "testAdjacentPaths/2014/11/18/00/57/24/015/2";
        String path3 = "testAdjacentPaths/2014/11/18/00/57/24/015/3";
        String nextSecond = "testAdjacentPaths/2014/11/18/00/57/25/015/1";

        spokeStore.insert(path1, BYTES);
        spokeStore.insert(path2, BYTES);
        spokeStore.insert(path3, BYTES);
        spokeStore.insert(previousSecond, BYTES);
        spokeStore.insert(nextSecond, BYTES);


        String previousMillisecond = "testAdjacentPaths/2014/11/18/00/57/24/014/1";
        spokeStore.insert(previousMillisecond, BYTES);
        String nextMillisecond = "testAdjacentPaths/2014/11/18/00/57/24/016/1";
        spokeStore.insert(nextMillisecond, BYTES);


        // filesInBucket tests
        Collection<String> keys = spokeStore.keysInBucket("/testAdjacentPaths/2014/11/18/00/57");
        assertEquals(7, keys.size());

        log.info("files " + keys);
        assertTrue(keys.contains(path1));
        assertTrue(keys.contains(path2));
        assertTrue(keys.contains(path3));

        // filesInBucket second query
        keys = spokeStore.keysInBucket("/testAdjacentPaths/2014/11/18/00/57/24");
        assertEquals(5, keys.size());

    }


    @Test
    public void testSpokeKeyFromFilePath() {
        final File file = new File(tempDir +
                "/test_0_7475501417648047/2014/11/19/18/15/43916UD7V4N");
        String key = spokeStore.spokeKeyFromPath(file.getAbsolutePath());
        assertEquals("test_0_7475501417648047/2014/11/19/18/15/43/916/UD7V4N", key);

        final File file1 = new File(tempDir +
                "/test_0_7475501417648047/2014/11/19/18");
        String directory = spokeStore.spokeKeyFromPath(file1.getAbsolutePath());
        assertEquals("test_0_7475501417648047/2014/11/19/18", directory);

    }

    @Test
    public void testLastFile() {
        DateTime time = new DateTime(2014, 12, 31, 23, 30, 1, 2, DateTimeZone.UTC);
        for (int i = 0; i < 30; i++) {
            time = time.plusMinutes(2);
            spokeStore.insert("testLastFile/" + new ContentKey(time, "A").toUrl(), BYTES);
            time = time.plusSeconds(1);
            spokeStore.insert("testLastFile/" + new ContentKey(time, "B").toUrl(), BYTES);
            time = time.plusMillis(1);
            spokeStore.insert("testLastFile/" + new ContentKey(time, "C").toUrl(), BYTES);
        }
        ContentKey limitKey = new ContentKey(time.minusMinutes(1), "A");
        String found = spokeStore.getLatest("testLastFile", limitKey.toUrl());
        assertEquals("testLastFile/2015/01/01/00/28/30/031/C", found);

        limitKey = new ContentKey(time, "B");
        found = spokeStore.getLatest("testLastFile", limitKey.toUrl());
        assertEquals("testLastFile/2015/01/01/00/30/31/031/B", found);

        limitKey = new ContentKey(time.plusMinutes(1), "D");
        found = spokeStore.getLatest("testLastFile", limitKey.toUrl());
        assertEquals("testLastFile/2015/01/01/00/30/31/032/C", found);
    }

    @Test
    public void testLatestBugNumber127() {
        String channel = "testBugNumber127";

        spokeStore.insert(channel + "/2015/03/17/17/31/13/686/2905180", BYTES);
        spokeStore.insert(channel + "/2015/03/17/17/31/43/691/2905200", BYTES);
        spokeStore.insert(channel + "/2015/03/17/17/31/59/600/2905220", BYTES);

        DateTime start = new DateTime(2015, 03, 17, 17, 37, 0, 0, DateTimeZone.UTC);
        String hash = "ZZZZZ";
        ContentKey limitKey = new ContentKey(start, hash);
        String found = spokeStore.getLatest(channel, limitKey.toUrl());
        log.info("found {}", found);
        assertEquals(channel + "/2015/03/17/17/31/59/600/2905220", found);

        limitKey = new ContentKey(start.plusSeconds(15), hash);
        found = spokeStore.getLatest(channel, limitKey.toUrl());
        log.info("found {}", found);
        assertEquals(channel + "/2015/03/17/17/31/59/600/2905220", found);

        limitKey = new ContentKey(start.plusSeconds(45), hash);
        found = spokeStore.getLatest(channel, limitKey.toUrl());
        log.info("found {}", found);
        assertEquals(channel + "/2015/03/17/17/31/59/600/2905220", found);
    }

    @Test
    public void testNextN() throws IOException {
        String name = "testNextN";

        DateTime startTime = TimeUtil.now().minusMinutes(59);
        DateTime time = startTime;
        for (int i = 0; i < 30; i++) {
            time = time.plusMinutes(2);
            spokeStore.insert(name + "/" + new ContentKey(time, "A").toUrl(), BYTES);
            time = time.plusSeconds(1);
            spokeStore.insert(name + "/" + new ContentKey(time, "B").toUrl(), BYTES);
            time = time.plusMillis(1);
            spokeStore.insert(name + "/" + new ContentKey(time, "C").toUrl(), BYTES);
        }
        ContentKey limitKey = new ContentKey(startTime, "A");

        List<String> found = getNextTesting(name, limitKey.toUrl(), 90);
        assertEquals(87, found.size());

        limitKey = new ContentKey(startTime.plusMinutes(30), "A");
        found = getNextTesting(name, limitKey.toUrl(), 45);
        assertEquals(45, found.size());
    }

    @Test
    public void testNextNFilterSeconds() throws IOException {
        String name = "testNextNFilterSeconds";

        DateTime startTime = TimeUtil.now().withSecondOfMinute(10).minusMinutes(10);
        ContentKey contentKeyA = new ContentKey(startTime, "A");
        spokeStore.insert(name + "/" + contentKeyA.toUrl(), BYTES);
        ContentKey contentKeyB = new ContentKey(startTime.plusSeconds(1), "B");
        spokeStore.insert(name + "/" + contentKeyB.toUrl(), BYTES);
        ContentKey contentKeyC = new ContentKey(startTime.plusSeconds(2), "C");
        spokeStore.insert(name + "/" + contentKeyC.toUrl(), BYTES);
        ContentKey contentKeyD = new ContentKey(startTime.plusSeconds(3), "D");
        spokeStore.insert(name + "/" + contentKeyD.toUrl(), BYTES);

        ContentKey limitKey = new ContentKey(startTime, "B");

        List<String> found = getNextTesting(name, limitKey.toUrl(), 2);
        log.info("found {}", found);
        assertEquals(3, found.size());
        assertTrue(contentKeyB.toUrl(), found.contains(name + "/" + contentKeyB.toUrl()));
        assertTrue(found.contains(name + "/" + contentKeyC.toUrl()));
    }

    @Test
    public void testNextNFilterMinutes() throws IOException {
        String name = "testNextNFilterMinutes";

        DateTime startTime = TimeUtil.now().minusMinutes(10);
        ContentKey contentKeyA = new ContentKey(startTime, "A");
        spokeStore.insert(name + "/" + contentKeyA.toUrl(), BYTES);
        ContentKey contentKeyB = new ContentKey(startTime.plusMinutes(1), "B");
        spokeStore.insert(name + "/" + contentKeyB.toUrl(), BYTES);
        ContentKey contentKeyC = new ContentKey(startTime.plusMinutes(2), "C");
        spokeStore.insert(name + "/" + contentKeyC.toUrl(), BYTES);
        ContentKey contentKeyD = new ContentKey(startTime.plusMinutes(3), "D");
        spokeStore.insert(name + "/" + contentKeyD.toUrl(), BYTES);

        ContentKey limitKey = new ContentKey(startTime, "B");

        List<String> found = getNextTesting(name, limitKey.toUrl(), 2);
        log.info("found {}", found);
        assertEquals(2, found.size());
        assertTrue(contentKeyB.toUrl(), found.contains(name + "/" + contentKeyB.toUrl()));
        assertTrue(found.contains(name + "/" + contentKeyC.toUrl()));
    }

    List<String> getNextTesting(String channel, String startKey, int count) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        spokeStore.getNext(channel, startKey, count, baos);
        String[] split = baos.toString().split(",");
        return Arrays.asList(split);
    }

    @Test
    public void testEnforceTtlYear() {
        enforceVerify("testEnforceTtlYear", new DateTime(2014, 12, 31, 23, 45, 1, 2, DateTimeZone.UTC));
    }

    @Test
    public void testEnforceTtlMonth() {
        enforceVerify("testEnforceTtlMonth", new DateTime(2015, 1, 31, 23, 45, 1, 2, DateTimeZone.UTC));
    }

    @Test
    public void testEnforceTtlDay() {
        enforceVerify("testEnforceTtlDay", new DateTime(2015, 2, 1, 23, 45, 1, 2, DateTimeZone.UTC));
    }

    @Test
    public void testEnforceTtlHour() {
        enforceVerify("testEnforceTtlHour", new DateTime(2015, 2, 1, 12, 45, 1, 2, DateTimeZone.UTC));
    }

    @Test
    public void testLatestBug() {
        DateTime now = TimeUtil.now();
        DateTime afterTheHour = now.withMinuteOfHour(0).withSecondOfMinute(0).withMillisOfSecond(1);
        DateTime beforeTheHour = now.minusHours(1).withMinuteOfHour(59).withSecondOfMinute(59).withMillisOfSecond(999);
        assertTrue(spokeStore.insert("testLatestBug/" + new ContentKey(afterTheHour, "0").toUrl(), BYTES));
        String beforeKey = new ContentKey(beforeTheHour, "0").toUrl();
        assertTrue(spokeStore.insert("testLatestBug/" + beforeKey, BYTES));
        DateTime limitTime = afterTheHour.withMillisOfSecond(0);
        String read = spokeStore.getLatest("testLatestBug", ContentKey.lastKey(limitTime).toUrl());
        assertNotNull(read);
        assertEquals("testLatestBug/" + beforeKey, read);
    }

    @Test
    public void testLatestCycle() {
        DateTime now = TimeUtil.now();
        String latest = spokeStore.getLatest("testLatestCycle", ContentKey.lastKey(now).toUrl());
        assertNull(latest);
        String key = new ContentKey(now, "0").toUrl();
        assertTrue(spokeStore.insert("testLatestCycle/" + key, BYTES));

        latest = spokeStore.getLatest("testLatestCycle", key);
        assertNull(latest);

        latest = spokeStore.getLatest("testLatestCycle", ContentKey.lastKey(now.plusMinutes(1)).toUrl());
        assertNotNull(latest);
        assertEquals("testLatestCycle/" + key, latest);
    }

    @Test
    public void testLatestBugStable() {
        /**
         * add one item before the latest hour
         * add one item after the latest hour
         * add one item after the limit key
         */
        DateTime now = TimeUtil.now();

        DateTime beforeTheHour = now.minusHours(1).withMinuteOfHour(59).withSecondOfMinute(59).withMillisOfSecond(999);
        String beforeKey = new ContentKey(beforeTheHour, "A").toUrl();
        assertTrue(spokeStore.insert("testLatestBugStable/" + beforeKey, BYTES));

        DateTime afterTheHour = now.withMinuteOfHour(0).withSecondOfMinute(0).withMillisOfSecond(1);
        String afterKey = new ContentKey(afterTheHour, "A").toUrl();
        assertTrue(spokeStore.insert("testLatestBugStable/" + afterKey, BYTES));

        String nowKey = new ContentKey(now, "0").toUrl();
        assertTrue(spokeStore.insert("testLatestBugStable/" + nowKey, BYTES));

        DateTime limitTime = now.minusSeconds(5);

        String read = spokeStore.getLatest("testLatestBugStable", ContentKey.lastKey(limitTime).toUrl());
        assertNotNull(read);
        assertEquals("testLatestBugStable/" + afterKey, read);
    }

    @Test
    public void testLatestMore() {
        DateTime now = TimeUtil.now();
        DateTime time = now;
        log.info("ttlMinutes {} ", ttlMinutes);
        DateTime ttlTime = time.minusMinutes(ttlMinutes);
        while (time.isAfter(ttlTime)) {
            spokeStore.insert("testLatestMore/" + new ContentKey(time, "A").toUrl(), BYTES);
            spokeStore.insert("testLatestMore/" + new ContentKey(time, "B").toUrl(), BYTES);
            time = time.minusMinutes(1);
        }

        time = now;
        while (time.isAfter(ttlTime)) {
            String read = spokeStore.getLatest("testLatestMore", ContentKey.lastKey(time).toUrl());
            assertNotNull(read);
            assertEquals("testLatestMore/" + new ContentKey(time, "B").toUrl(), read);
            time = time.minusMinutes(1);
        }
    }

    private void enforceVerify(String channel, DateTime startTime) {
        DateTime time = startTime;
        String startQuery = TimeUtil.hours(time);
        for (int i = 0; i < 30; i++) {
            time = time.plusMinutes(1);
            spokeStore.insert(channel + "/" + new ContentKey(time, "" + i).toUrl(), BYTES);
        }
        String endQuery = TimeUtil.hours(time);
        verify(channel + "/" + startQuery, 14);
        verify(channel + "/" + endQuery, 16);

        spokeStore.enforceTtl(channel, startTime.plusMinutes(17));
        verify(channel + "/" + startQuery, 0);
        verify(channel + "/" + endQuery, 13);
    }

    private void verify(String path, int expected) {
        ArrayList<ContentKey> keys = new ArrayList<>();
        ContentKeyUtil.convertKeyStrings(spokeStore.readKeysInBucket(path), keys);
        assertEquals(expected, keys.size());
    }

}
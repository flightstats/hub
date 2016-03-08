package com.flightstats.hub.spoke;

import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.ContentKeyUtil;
import com.flightstats.hub.util.TimeUtil;
import com.google.common.io.Files;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.*;

public class FileSpokeStoreTest {
    public static final byte[] BYTES = new byte[]{0, 2, 3, 4, 5, 6};
    private final static Logger logger = LoggerFactory.getLogger(FileSpokeStoreTest.class);
    private String tempDir;
    private FileSpokeStore spokeStore;
    private static final int ttlMinutes = HubProperties.getProperty("spoke.ttlMinutes", 60);

    @Before
    public void setUp() throws Exception {
        tempDir = Files.createTempDir().getPath();
        spokeStore = new FileSpokeStore(tempDir);
    }

    @Test
    public void testWriteRead() throws Exception {
        String path = "channelWR/" + new ContentKey().toUrl();
        assertTrue(spokeStore.write(path, BYTES));
        byte[] read = spokeStore.read(path);
        assertArrayEquals(BYTES, read);
    }

    @Test
    public void testPathTranslation() throws Exception {
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
    public void testAdjacentPaths() throws Exception {
        String previousSecond = "testAdjacentPaths/2014/11/18/00/57/23/015/1";
        String path1 = "testAdjacentPaths/2014/11/18/00/57/24/015/1";
        String path2 = "testAdjacentPaths/2014/11/18/00/57/24/015/2";
        String path3 = "testAdjacentPaths/2014/11/18/00/57/24/015/3";
        String nextSecond = "testAdjacentPaths/2014/11/18/00/57/25/015/1";

        spokeStore.write(path1, BYTES);
        spokeStore.write(path2, BYTES);
        spokeStore.write(path3, BYTES);
        spokeStore.write(previousSecond, BYTES);
        spokeStore.write(nextSecond, BYTES);


        String previousMillisecond = "testAdjacentPaths/2014/11/18/00/57/24/014/1";
        spokeStore.write(previousMillisecond, BYTES);
        String nextMillisecond = "testAdjacentPaths/2014/11/18/00/57/24/016/1";
        spokeStore.write(nextMillisecond, BYTES);


        // filesInBucket tests
        Collection<String> keys = spokeStore.keysInBucket("/testAdjacentPaths/2014/11/18/00/57");
        assertEquals(7, keys.size());

        logger.info("files " + keys);
        assertTrue(keys.contains(path1));
        assertTrue(keys.contains(path2));
        assertTrue(keys.contains(path3));

        // filesInBucket second query
        keys = spokeStore.keysInBucket("/testAdjacentPaths/2014/11/18/00/57/24");
        assertEquals(5, keys.size());

    }


    @Test
    public void testSpokeKeyFromFilePath() throws Exception {
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
            spokeStore.write("testLastFile/" + new ContentKey(time, "A").toUrl(), BYTES);
            time = time.plusSeconds(1);
            spokeStore.write("testLastFile/" + new ContentKey(time, "B").toUrl(), BYTES);
            time = time.plusMillis(1);
            spokeStore.write("testLastFile/" + new ContentKey(time, "C").toUrl(), BYTES);
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

        spokeStore.write(channel + "/2015/03/17/17/31/13/686/2905180", BYTES);
        spokeStore.write(channel + "/2015/03/17/17/31/43/691/2905200", BYTES);
        spokeStore.write(channel + "/2015/03/17/17/31/59/600/2905220", BYTES);

        DateTime start = new DateTime(2015, 03, 17, 17, 37, 0, 0, DateTimeZone.UTC);
        String hash = "ZZZZZ";
        ContentKey limitKey = new ContentKey(start, hash);
        String found = spokeStore.getLatest(channel, limitKey.toUrl());
        logger.info("found {}", found);
        assertEquals(channel + "/2015/03/17/17/31/59/600/2905220", found);

        limitKey = new ContentKey(start.plusSeconds(15), hash);
        found = spokeStore.getLatest(channel, limitKey.toUrl());
        logger.info("found {}", found);
        assertEquals(channel + "/2015/03/17/17/31/59/600/2905220", found);

        limitKey = new ContentKey(start.plusSeconds(45), hash);
        found = spokeStore.getLatest(channel, limitKey.toUrl());
        logger.info("found {}", found);
        assertEquals(channel + "/2015/03/17/17/31/59/600/2905220", found);
    }

    @Test
    public void testNextN() throws IOException {
        String name = "testNextN";

        DateTime startTime = TimeUtil.now().minusMinutes(59);
        DateTime time = startTime;
        for (int i = 0; i < 30; i++) {
            time = time.plusMinutes(2);
            spokeStore.write(name + "/" + new ContentKey(time, "A").toUrl(), BYTES);
            time = time.plusSeconds(1);
            spokeStore.write(name + "/" + new ContentKey(time, "B").toUrl(), BYTES);
            time = time.plusMillis(1);
            spokeStore.write(name + "/" + new ContentKey(time, "C").toUrl(), BYTES);
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
        spokeStore.write(name + "/" + contentKeyA.toUrl(), BYTES);
        ContentKey contentKeyB = new ContentKey(startTime.plusSeconds(1), "B");
        spokeStore.write(name + "/" + contentKeyB.toUrl(), BYTES);
        ContentKey contentKeyC = new ContentKey(startTime.plusSeconds(2), "C");
        spokeStore.write(name + "/" + contentKeyC.toUrl(), BYTES);
        ContentKey contentKeyD = new ContentKey(startTime.plusSeconds(3), "D");
        spokeStore.write(name + "/" + contentKeyD.toUrl(), BYTES);

        ContentKey limitKey = new ContentKey(startTime, "B");

        List<String> found = getNextTesting(name, limitKey.toUrl(), 2);
        logger.info("found {}", found);
        assertEquals(3, found.size());
        assertTrue(contentKeyB.toUrl(), found.contains(name + "/" + contentKeyB.toUrl()));
        assertTrue(found.contains(name + "/" + contentKeyC.toUrl()));
    }

    @Test
    public void testNextNFilterMinutes() throws IOException {
        String name = "testNextNFilterMinutes";

        DateTime startTime = TimeUtil.now().minusMinutes(10);
        ContentKey contentKeyA = new ContentKey(startTime, "A");
        spokeStore.write(name + "/" + contentKeyA.toUrl(), BYTES);
        ContentKey contentKeyB = new ContentKey(startTime.plusMinutes(1), "B");
        spokeStore.write(name + "/" + contentKeyB.toUrl(), BYTES);
        ContentKey contentKeyC = new ContentKey(startTime.plusMinutes(2), "C");
        spokeStore.write(name + "/" + contentKeyC.toUrl(), BYTES);
        ContentKey contentKeyD = new ContentKey(startTime.plusMinutes(3), "D");
        spokeStore.write(name + "/" + contentKeyD.toUrl(), BYTES);

        ContentKey limitKey = new ContentKey(startTime, "B");

        List<String> found = getNextTesting(name, limitKey.toUrl(), 2);
        logger.info("found {}", found);
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
        assertTrue(spokeStore.write("testLatestBug/" + new ContentKey(afterTheHour, "0").toUrl(), BYTES));
        String beforeKey = new ContentKey(beforeTheHour, "0").toUrl();
        assertTrue(spokeStore.write("testLatestBug/" + beforeKey, BYTES));
        DateTime limitTime = afterTheHour.withMillisOfSecond(0);
        String read = spokeStore.getLatest("testLatestBug", ContentKey.lastKey(limitTime).toUrl());
        assertNotNull(read);
        assertEquals("testLatestBug/" + beforeKey, read);
    }

    @Test
    public void testLatestBugBeforeTtl() {
        DateTime now = TimeUtil.now();
        DateTime afterTheHour = now.withMinuteOfHour(0).withSecondOfMinute(0).withMillisOfSecond(1);
        DateTime beforeTheHour = now.minusHours(1).withMinuteOfHour(59).withSecondOfMinute(59).withMillisOfSecond(999);
        DateTime beforeTheTtl = now.minusMinutes(ttlMinutes).minusHours(1).withMinuteOfHour(59).withSecondOfMinute(59).withMillisOfSecond(999);
        assertTrue(spokeStore.write("testLatestBug/" + new ContentKey(afterTheHour, "0").toUrl(), BYTES));
        assertTrue(spokeStore.write("testLatestBug/" + new ContentKey(beforeTheTtl, "0").toUrl(), BYTES));
        String beforeKey = new ContentKey(beforeTheHour, "0").toUrl();
        assertTrue(spokeStore.write("testLatestBug/" + beforeKey, BYTES));
        DateTime limitTime = beforeTheHour.minusMinutes(1);
        String read = spokeStore.getLatest("testLatestBug", ContentKey.lastKey(limitTime).toUrl());
        assertNull(read);
    }

    private void enforceVerify(String channel, DateTime startTime) {
        DateTime time = startTime;
        String startQuery = TimeUtil.hours(time);
        for (int i = 0; i < 30; i++) {
            time = time.plusMinutes(1);
            spokeStore.write(channel + "/" + new ContentKey(time, "" + i).toUrl(), BYTES);
        }
        String endQuery = TimeUtil.hours(time);
        verify(channel + "/" + startQuery, 14);
        verify(channel + "/" + endQuery, 16);

        spokeStore.enforceTtl(channel, startTime.plusMinutes(17));
        verify(channel + "/" + startQuery, 0);
        verify(channel + "/" + endQuery, 13);
    }

    private void verify(String path, int expected) {
        String keysInBucket = spokeStore.readKeysInBucket(path);
        ArrayList<ContentKey> keys = new ArrayList();
        ContentKeyUtil.convertKeyStrings(spokeStore.readKeysInBucket(path), keys);
        assertEquals(expected, keys.size());
    }

}
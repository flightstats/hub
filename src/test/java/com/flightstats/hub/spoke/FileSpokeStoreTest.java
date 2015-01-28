package com.flightstats.hub.spoke;

import com.flightstats.hub.model.ContentKey;
import com.google.common.io.Files;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Collection;

import static org.junit.Assert.*;

public class FileSpokeStoreTest {
    public static final byte[] BYTES = new byte[]{0, 2, 3, 4, 5, 6};
    private final static Logger logger = LoggerFactory.getLogger(FileSpokeStoreTest.class);
    private String tempDir;
    private FileSpokeStore spokeStore;

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
        String found = spokeStore.getLast("testLastFile");
        assertEquals("testLastFile/2015/01/01/00/30/31/032/C", found);
    }

}
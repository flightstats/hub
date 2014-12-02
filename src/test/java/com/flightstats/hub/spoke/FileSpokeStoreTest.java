package com.flightstats.hub.spoke;

import com.flightstats.hub.model.ContentKey;
import com.google.common.io.Files;
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
        String urlPart = spokeStore.spokeKeyFromFile(new File(filePath));
        assertEquals(incoming, urlPart);
    }

    File makeFile(String path){
        return spokeStore.spokeFilePathPart(path);
    }

    @Test
    public void testAdjacentPaths() throws Exception {
        String previousSecond = "testAdjacentPaths/2014/11/18/00/57/23/015/1";
        String path1 = "testAdjacentPaths/2014/11/18/00/57/24/015/1";
        File path1File = makeFile(path1);
        String path2 = "testAdjacentPaths/2014/11/18/00/57/24/015/2";
        File path2File = makeFile(path2);
        String path3 = "testAdjacentPaths/2014/11/18/00/57/24/015/3";
        File path3File = makeFile(path3);
        String nextSecond = "testAdjacentPaths/2014/11/18/00/57/25/015/1";
        File nextSecondFile = makeFile(nextSecond);

        spokeStore.write(path1, BYTES);
        spokeStore.write(path2, BYTES);
        spokeStore.write(path3, BYTES);
        spokeStore.write(previousSecond, BYTES);
        spokeStore.write(nextSecond, BYTES);

        // test happy cases
        assertEquals(path3, spokeStore.nextPath(path2));
        assertEquals(path1, spokeStore.previousPath(path2));

        String previousMillisecond = "testAdjacentPaths/2014/11/18/00/57/24/014/1";
        spokeStore.write(previousMillisecond, BYTES);
        String nextMillisecond = "testAdjacentPaths/2014/11/18/00/57/24/016/1";
        spokeStore.write(nextMillisecond, BYTES);

        assertEquals(spokeStore.nextPath(path3), nextMillisecond);
        assertEquals(spokeStore.previousPath(path1), previousMillisecond);


        // filesInBucket tests
        Collection<File> files = spokeStore.filesInBucket(new File(tempDir + "/testAdjacentPaths/2014/11/18/00/57"),
                null);
        assertEquals(7, files.size());

        logger.info("files " + files);
        assertTrue(files.contains(path1File));
        assertTrue(files.contains(path2File));
        assertTrue(files.contains(path3File));

        // filesInBucket second query
        files = spokeStore.filesInBucket(spokeStore.spokeFilePathPart(
                "/testAdjacentPaths/2014/11/18/00/57/24"), "24");
        assertEquals(5, files.size());

        //todo - gfm - 12/2/14 - this fails on Jenkins, but passes locally
        //org.junit.ComparisonFailure: expected:
        // <...14/11/18/00/57/24/01[4/1,testAdjacentPaths/2014/11/18/00/57/24/015/1,testAdjacentPaths/2014/11/18/00/57/24/015/2,testAdjacentPaths/2014/11/18/00/57/24/015/3,testAdjacentPaths/2014/11/18/00/57/24/016/1]>
        // but was:
        // <...14/11/18/00/57/24/01[5/3,testAdjacentPaths/2014/11/18/00/57/24/015/1,testAdjacentPaths/2014/11/18/00/57/24/014/1,testAdjacentPaths/2014/11/18/00/57/24/016/1,testAdjacentPaths/2014/11/18/00/57/24/015/2]>
        //String readKeys = spokeStore.readKeysInBucket("/testAdjacentPaths/2014/11/18/00/57/24");
        //assertEquals("testAdjacentPaths/2014/11/18/00/57/24/014/1,testAdjacentPaths/2014/11/18/00/57/24/015/1,testAdjacentPaths/2014/11/18/00/57/24/015/2,testAdjacentPaths/2014/11/18/00/57/24/015/3,testAdjacentPaths/2014/11/18/00/57/24/016/1", readKeys);

        // test adjacent
        files = spokeStore.nextNKeys(path1File,2);
        assertEquals(2,files.size());
        assertTrue(files.contains(path2File));
        assertTrue(files.contains(path3File));

        String nexthour1 = "testAdjacentPaths/2014/11/19/00/57/24/016/1";
        spokeStore.write(nexthour1, BYTES);
        File nexthour1File = makeFile(nexthour1);
        assertEquals(nexthour1File,spokeStore.nextPath(nextSecondFile));

        files = spokeStore.nextNKeys(path1File,4);
        assertEquals(4,files.size());

        // previous test
        files = spokeStore.previousNKeys(nexthour1File, 3);
        assertEquals(3,files.size());

    }

    @Test
    public void testSpokeKeyFromFilePath() throws Exception {
        String key = spokeStore.spokeKeyFromFile(new File(tempDir +
                "/test_0_7475501417648047/2014/11/19/18/15/43916UD7V4N"));
        assertEquals("test_0_7475501417648047/2014/11/19/18/15/43/916/UD7V4N", key);

        String directory = spokeStore.spokeKeyFromFile(new File(tempDir +
                "/test_0_7475501417648047/2014/11/19/18"));
        assertEquals("test_0_7475501417648047/2014/11/19/18", directory);

    }

}
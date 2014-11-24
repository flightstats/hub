package com.flightstats.hub.spoke;

import com.flightstats.hub.model.ContentKey;
import com.google.common.io.Files;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        String incoming = "test_0_4274725520517677/2014/11/18/00/57/24/015/NV2cl5";
        String output = spokeStore.spokeFilePathPart(incoming);
        String filePath = "/test_0_4274725520517677/2014/11/18/00/57/24015NV2cl5";
        assertEquals(tempDir + filePath, output);
        String urlPart = spokeStore.spokeKeyFromFilePath(filePath);
        assertEquals(incoming, urlPart);
    }

    @Test
    public void testAdjacentPaths() throws Exception {
        String path1 = "testAdjacentPaths/2014/11/18/00/57/24/015/1";
        String path2 = "testAdjacentPaths/2014/11/18/00/57/24/015/2";
        String path3 = "testAdjacentPaths/2014/11/18/00/57/24/015/3";
        spokeStore.write(path1, BYTES);
        spokeStore.write(path2, BYTES);
        spokeStore.write(path3, BYTES);

        // test happy cases
        assertEquals(path3, spokeStore.nextPath(path2));
        assertEquals(path1, spokeStore.previousPath(path2));

        String previousbucket1 = "testAdjacentPaths/2014/11/18/00/57/24/014/1";
        spokeStore.write(previousbucket1, BYTES);
        String nextbucket1 = "testAdjacentPaths/2014/11/18/00/57/24/016/1";
        spokeStore.write(nextbucket1, BYTES);

        assertEquals(spokeStore.nextPath(path3), nextbucket1);
        assertEquals(spokeStore.previousPath(path1), previousbucket1);

        // keysInBucket tests
        Collection<String> files = spokeStore.keysInBucket(tempDir + "/testAdjacentPaths/2014/11/18/00/57");
        assertEquals(5, files.size());

        logger.info("files " + files);
        assertTrue(files.contains(path1));
        assertTrue(files.contains(path2));
        assertTrue(files.contains(path3));

        // keysInBucket second query
        files = spokeStore.keysInBucket(tempDir + "/testAdjacentPaths/2014/11/18/00/57/24");
        assertEquals(5, files.size());

        // test adjacent
        files = spokeStore.nextNKeys(path1,2);
        assertEquals(2,files.size());

    }

    @Test
    public void testSpokeKeyFromFilePath() throws Exception {
        String key = spokeStore.spokeKeyFromFilePath(tempDir + "/test_0_7475501417648047/2014/11/19/18/15/43916UD7V4N");
        assertEquals("test_0_7475501417648047/2014/11/19/18/15/43/916/UD7V4N", key);
    }

}
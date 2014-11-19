package com.flightstats.hub.spoke;

import com.flightstats.hub.model.ContentKey;
import com.google.common.io.Files;
import org.junit.Test;

import java.io.File;
import java.util.Collection;

import static org.junit.Assert.*;

public class FileSpokeStoreTest {

    @Test
    public void testWriteRead() throws Exception {
        File tempDir = Files.createTempDir();
        FileSpokeStore spokeStore = new FileSpokeStore(tempDir.getPath());

        byte[] bytes = {0, 2, 3, 4, 5, 6};
        String path = "channelWR/" + new ContentKey().toUrl();
        assertTrue(spokeStore.write(path, bytes));
        byte[] read = spokeStore.read(path);
        assertArrayEquals(bytes, read);
    }

    @Test
    public void testPathTranslation() throws Exception {
        String tempDir = Files.createTempDir().getPath();
        String incoming = "test_0_4274725520517677/2014/11/18/00/57/24/015/NV2cl5";
        FileSpokeStore spokeStore = new FileSpokeStore(tempDir);
        String output = spokeStore.spokeFilePathPart(incoming);
        String filePath = "/test_0_4274725520517677/2014/11/18/00/57/24015NV2cl5";
        assertEquals(tempDir + filePath, output);
        String urlPart = spokeStore.spokeKeyFromFilePath(filePath);
        assertEquals(incoming, urlPart);
    }



   @Test public void testAdjacentPaths() throws Exception{
        // setup - folder with 3 files
        File tempDir = Files.createTempDir();
        FileSpokeStore FileSpokeStore = new FileSpokeStore(tempDir.getPath());
        byte[] bytes = {0, 2, 3, 4, 5, 6};

        String path1 = "testAdjacentPaths/2014/11/18/00/57/24/015/1";
        String path2 = "testAdjacentPaths/2014/11/18/00/57/24/015/2";
        String path3 = "testAdjacentPaths/2014/11/18/00/57/24/015/3";
        FileSpokeStore.write(path1, bytes);
        FileSpokeStore.write(path2, bytes);
        FileSpokeStore.write(path3, bytes);

        // test happy cases
        assertEquals(path3,FileSpokeStore.nextPath(path2));
        assertEquals(path1,FileSpokeStore.previousPath(path2));

        // test adjacent bucket cases
        String previousbucket1 = "testAdjacentPaths/2014/11/18/00/57/24/014/1";
        FileSpokeStore.write(previousbucket1, bytes);
        String nextbucket1 = "testAdjacentPaths/2014/11/18/00/57/24/016/1";
        FileSpokeStore.write(nextbucket1, bytes);

        assertEquals(FileSpokeStore.nextPath(path3), nextbucket1);
        assertEquals(FileSpokeStore.previousPath(path1), previousbucket1);

        // keysInBucket tests
        Collection<String> files = FileSpokeStore.keysInBucket("testAdjacentPaths/2014/11/18/00/57");
        assertEquals(5, files.size());
        files = FileSpokeStore.keysInBucket("testAdjacentPaths/2014/11/18/00/57");
        assertEquals(5, files.size());
    }

}
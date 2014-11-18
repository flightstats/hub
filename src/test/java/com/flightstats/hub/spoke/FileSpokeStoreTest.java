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
        String output = spokeStore.spokePath(incoming);
        assertEquals(tempDir + "/test_0_4274725520517677/2014/11/18/00/57/24015NV2cl5", output);
    }

   @Test public void testAdjacentPaths() throws Exception{
        // setup - folder with 3 files
        File tempDir = Files.createTempDir();
        FileSpokeStore FileSpokeStore = new FileSpokeStore(tempDir.getPath());
        byte[] bytes = {0, 2, 3, 4, 5, 6};
        String path1 = "a/b/c/10/1.txt";
        String path2 = "a/b/c/10/2.txt";
        String path3 = "a/b/c/10/3.txt";
        FileSpokeStore.write(path1, bytes);
        FileSpokeStore.write(path2, bytes);
        FileSpokeStore.write(path3, bytes);

        // test happy cases
        assertEquals(FileSpokeStore.nextPath(path2), path3);
        assertEquals(FileSpokeStore.previousPath(path2), path1);

        // test adjacent bucket cases
        String previousbucket1 = "a/b/c/09/1.txt";
        FileSpokeStore.write(previousbucket1, bytes);
        String nextbucket1 = "a/b/c/11/1.txt";
        FileSpokeStore.write(nextbucket1, bytes);

        assertEquals(FileSpokeStore.nextPath(path3), nextbucket1);
        assertEquals(FileSpokeStore.previousPath(path1), previousbucket1);

       // keysInBucket tests
        Collection<String> files = FileSpokeStore.keysInBucket("a/b/c");
        assertEquals(files.size(), 5);
        files = FileSpokeStore.keysInBucket("a/b/c/10");
        assertEquals(files.size(), 3);
    }

}
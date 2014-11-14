package com.flightstats.hub.spoke;

import com.google.common.io.Files;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

public class FileSpokeStoreTest {

    @Test
    public void testWriteRead() throws Exception {
        File tempDir = Files.createTempDir();
        SpokeStore spokeStore = new FileSpokeStore(tempDir.getPath());

        byte[] bytes = {0, 2, 3, 4, 5, 6};
        String path = "a/b/c/10/blah";
        assertTrue(spokeStore.write(path, bytes));
        byte[] read = spokeStore.read(path);
        assertArrayEquals(bytes, read);
    }
}
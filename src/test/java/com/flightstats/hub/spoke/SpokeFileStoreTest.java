package com.flightstats.hub.spoke;

import com.google.common.io.Files;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

public class SpokeFileStoreTest {

    @Test
    public void testWriteRead() throws Exception {
        File tempDir = Files.createTempDir();
        SpokeFileStore spokeFileStore = new SpokeFileStore(tempDir.getPath());

        byte[] bytes = {0, 2, 3, 4, 5, 6};
        String path = "a/b/c/10/blah";
        assertTrue(spokeFileStore.write(path, bytes));
        byte[] read = spokeFileStore.read(path);
        assertArrayEquals(bytes, read);
    }
}
package com.flightstats.hub.model;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ContentKeyTest {

    @Test
    public void testStorageKey() throws Exception {
        ContentKey contentKey = new ContentKey();
        ContentKey cycled = ContentKey.fromStorage(contentKey.toStorage()).get();
        assertEquals(contentKey, cycled);
    }

    @Test
    public void testUrlKey() throws Exception {
        ContentKey contentKey = new ContentKey();
        ContentKey cycled = ContentKey.fromUrl(contentKey.toUrl()).get();
        assertEquals(contentKey, cycled);
    }
}

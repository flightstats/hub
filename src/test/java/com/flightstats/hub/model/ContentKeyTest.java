package com.flightstats.hub.model;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ContentKeyTest {

    @Test
    public void testKeyToString() throws Exception {
        ContentKey contentKey = new ContentKey();
        ContentKey cycled = ContentKey.fromString(contentKey.key()).get();
        assertEquals(contentKey, cycled);
    }
}

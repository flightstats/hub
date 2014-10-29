package com.flightstats.hub.model;

import com.google.common.base.Optional;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 *
 */
public class ContentKeyTest {

    @Test
    public void testKeyToString() throws Exception {
        ContentKey contentKey = new ContentKey();
        Optional<ContentKey> cycled = ContentKey.fromString(contentKey.key());
        System.out.println(contentKey.keyToUrl());
        System.out.println(cycled.get().keyToUrl());
        assertEquals(contentKey, cycled.get());
    }
}

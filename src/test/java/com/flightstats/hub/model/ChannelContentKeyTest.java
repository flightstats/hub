package com.flightstats.hub.model;

import org.junit.jupiter.api.Test;

import java.util.SortedSet;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ChannelContentKeyTest {

    @Test
    public void test() {
        ContentKey contentKey = new ContentKey();
        ChannelContentKey A1 = new ChannelContentKey("A", contentKey);
        ChannelContentKey B1 = new ChannelContentKey("B", contentKey);
        ChannelContentKey A2 = new ChannelContentKey("A", new ContentKey(contentKey.getTime().plusMinutes(1), contentKey.getHash()));

        SortedSet<ChannelContentKey> orderedKeys = new TreeSet<>();
        orderedKeys.add(A2);
        orderedKeys.add(A1);
        orderedKeys.add(B1);

        assertEquals(3, orderedKeys.size());
        assertEquals(A1, orderedKeys.first());
        assertEquals(A2, orderedKeys.last());
    }

    @Test
    public void testCycle() {
        ContentKey contentKey = new ContentKey();
        ChannelContentKey channelContentKey = new ChannelContentKey("name", contentKey);
        ChannelContentKey cycled = ChannelContentKey.fromResourcePath(channelContentKey.toUrl());
        assertEquals(channelContentKey, cycled);
    }

    @Test
    public void fromResourcePath() {
        String url = "http://hub/channel/foo/1999/12/31/23/59/59/999/l33t";
        ChannelContentKey key = ChannelContentKey.fromResourcePath(url);
        assertEquals(key.getChannel(), "foo");
        assertEquals(key.getContentKey(), new ContentKey(1999, 12, 31, 23, 59, 59, 999, "l33t"));
    }

    @Test
    public void fromSpokePathValid() {
        String filePath = "/some/arbitrary/directory/structure/foo/1999/12/31/23/59/59999l33t";
        ChannelContentKey key = ChannelContentKey.fromSpokePath(filePath);
        assertEquals(key.getChannel(), "foo");
        assertEquals(key.getContentKey(), new ContentKey(1999, 12, 31, 23, 59, 59, 999, "l33t"));
    }

    @Test
    public void fromSpokePathInvalidFormat() {
        try {
            String filePath = "/some/arbitrary/directory/structure/foo/1999/12/31/bar/23/59/59999l33t";
                    ChannelContentKey.fromSpokePath(filePath);
        } catch (Exception ex) {
            assertEquals(ex.getClass(), IllegalArgumentException.class);
        }
    }

    @Test
    public void fromSpokePathInvalidDepth() {
        try {
            String filePath = "/foo/bar/23/59/59999l33t";
            ChannelContentKey.fromSpokePath(filePath);
        } catch (Exception ex) {
            assertEquals(ex.getClass(), IllegalArgumentException.class);
        }

    }

    @Test
    public void fromChannelPath() {
        String path = "foo/1999/12/31/23/59/59/999/l33t";
        ChannelContentKey key = ChannelContentKey.fromChannelPath(path);
        assertNotNull(key);
        assertEquals(key.getChannel(), "foo");
        assertEquals(key.getContentKey(), new ContentKey(1999, 12, 31, 23, 59, 59, 999, "l33t"));
    }
}
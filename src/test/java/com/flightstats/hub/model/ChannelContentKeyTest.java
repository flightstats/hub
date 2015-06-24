package com.flightstats.hub.model;

import org.junit.Test;

import java.util.SortedSet;
import java.util.TreeSet;

import static org.junit.Assert.assertEquals;

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
}
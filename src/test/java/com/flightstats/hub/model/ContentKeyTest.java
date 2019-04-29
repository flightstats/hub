package com.flightstats.hub.model;

import com.flightstats.hub.util.TimeUtil;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;

import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.*;

class ContentKeyTest {

    @Test
    void testUrlKey() throws Exception {
        ContentKey contentKey = new ContentKey();
        ContentKey cycled = ContentKey.fromUrl(contentKey.toUrl()).get();
        assertEquals(contentKey, cycled);
    }

    @Test
    void testCompareTime() throws Exception {
        DateTime now = TimeUtil.now();
        TreeSet<ContentKey> keys = new TreeSet<>();
        for (int i = 0; i < 10; i++) {
            keys.add(new ContentKey(now.plusMinutes(i), "A"));
        }
        assertEquals(10, keys.size());
        long previous = 0;
        for (ContentKey key : keys) {
            long current = key.getMillis();
            assertTrue(current > previous);
            previous = current;
        }
    }

    @Test
    void testCompareHash() throws Exception {
        DateTime now = TimeUtil.now();
        TreeSet<ContentKey> keys = new TreeSet<>();
        for (int i = 0; i < 10; i++) {
            keys.add(new ContentKey(now, "A" + i));
        }
        assertEquals(10, keys.size());
        String previous = "";
        for (ContentKey key : keys) {
            String current = key.toUrl();
            assertTrue(current.compareTo(previous) > 0);
            previous = current;
        }
    }

    @Test
    void testZkCycle() throws Exception {
        ContentKey key = new ContentKey();
        assertEquals(key, key.fromZk(key.toZk()));
    }

    @Test
    void testFullUrl() {
        ContentKey contentKey = ContentKey.fromFullUrl("http://hub/channel/load_test_2/2015/01/23/21/11/19/407/L7QtaY");
        assertNotNull(contentKey);
        assertEquals("2015/01/23/21/11/19/407/L7QtaY", contentKey.toString());
    }

    @Test
    void testCompareMinutePath() {
        MinutePath minutePath = new MinutePath();
        ContentKey contentKey = new ContentKey(minutePath.getTime(), "0");
        assertTrue(contentKey.compareTo(minutePath) < 0);

        ContentKey nextSeconds = new ContentKey(minutePath.getTime().plusSeconds(59), "0");
        assertTrue(nextSeconds.compareTo(minutePath) < 0);

        ContentKey nextMinute = new ContentKey(minutePath.getTime().plusMinutes(1), "0");
        assertTrue(nextMinute.compareTo(minutePath) > 0);
    }
}


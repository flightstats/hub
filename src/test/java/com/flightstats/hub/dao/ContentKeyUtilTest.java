package com.flightstats.hub.dao;

import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.MinutePath;
import com.flightstats.hub.util.TimeUtil;
import org.joda.time.DateTime;
import org.junit.Test;

import java.util.SortedSet;
import java.util.TreeSet;

import static org.junit.Assert.assertEquals;

public class ContentKeyUtilTest {

    @Test
    public void testConvertKeysToMinutes() {
        DateTime start = TimeUtil.now();
        SortedSet<ContentKey> keys = new TreeSet<>();
        for (int i = 0; i < 100; i += 2) {
            keys.add(new ContentKey(start.plusMinutes(i), "A" + i));
            keys.add(new ContentKey(start.plusMinutes(i), "B" + i));
        }
        SortedSet<MinutePath> minutePaths = ContentKeyUtil.convert(keys);
        assertEquals(50, minutePaths.size());
        for (MinutePath minutePath : minutePaths) {
            assertEquals(2, minutePath.getKeys().size());
            for (ContentKey key : minutePath.getKeys()) {
                assertEquals(minutePath.getTime(), new MinutePath(key.getTime()).getTime());
            }
        }
    }
}
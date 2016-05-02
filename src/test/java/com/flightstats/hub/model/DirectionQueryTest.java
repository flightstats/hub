package com.flightstats.hub.model;

import com.flightstats.hub.util.TimeUtil;
import org.joda.time.DateTime;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DirectionQueryTest {

    @Test
    public void testOutsideOfCachePrevious() {
        DateTime start = TimeUtil.now();
        ContentKey contentKey = new ContentKey(start, "a");
        DirectionQuery query = DirectionQuery.builder()
                .contentKey(contentKey)
                .next(false)
                .build();
        assertTrue(query.outsideOfCache(start));
        assertTrue(query.outsideOfCache(start.plusMillis(1)));
        assertTrue(query.outsideOfCache(start.minusMillis(1)));
    }

    @Test
    public void testOutsideOfCacheNext() {
        DateTime start = TimeUtil.now();
        ContentKey contentKey = new ContentKey(start, "a");
        DirectionQuery query = DirectionQuery.builder()
                .contentKey(contentKey)
                .next(true)
                .build();
        assertFalse(query.outsideOfCache(start));
        assertTrue(query.outsideOfCache(start.plusMillis(1)));
        assertFalse(query.outsideOfCache(start.minusMillis(1)));
    }

}
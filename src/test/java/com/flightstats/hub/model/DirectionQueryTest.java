package com.flightstats.hub.model;

import com.flightstats.hub.util.TimeUtil;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

 class DirectionQueryTest {

    @Test
     void testOutsideOfCachePrevious() {
        DateTime start = TimeUtil.now();
        ContentKey contentKey = new ContentKey(start, "a");
        DirectionQuery query = DirectionQuery.builder()
                .startKey(contentKey)
                .next(false)
                .build();
        assertTrue(query.outsideOfCache(start));
        assertTrue(query.outsideOfCache(start.plusMillis(1)));
        assertTrue(query.outsideOfCache(start.minusMillis(1)));
    }

    @Test
     void testOutsideOfCacheNext() {
        DateTime start = TimeUtil.now();
        ContentKey contentKey = new ContentKey(start, "a");
        DirectionQuery query = DirectionQuery.builder()
                .startKey(contentKey)
                .next(true)
                .build();
        assertFalse(query.outsideOfCache(start));
        assertTrue(query.outsideOfCache(start.plusMillis(1)));
        assertFalse(query.outsideOfCache(start.minusMillis(1)));
    }

}
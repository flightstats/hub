package com.flightstats.hub.webhook.strategy;

import com.flightstats.hub.model.TimeQuery;
import com.flightstats.hub.util.TimeUtil;
import com.flightstats.hub.webhook.strategy.QueryGenerator;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QueryGeneratorTest {

    @Test
    void testNormal() {
        DateTime startTime = new DateTime(2015, 5, 7, 11, 5, 1, 2, DateTimeZone.UTC);
        DateTime latestStableInChannel = new DateTime(2015, 5, 7, 11, 6, 1, 2, DateTimeZone.UTC);
        QueryGenerator generator = new QueryGenerator(startTime, "test");

        TimeQuery query = generator.getQuery(latestStableInChannel);
        assertEquals(TimeUtil.Unit.SECONDS, query.getUnit());
        assertEquals("2015/05/07/11/05/01/002/", TimeUtil.millis(query.getStartTime()));

        query = generator.getQuery(latestStableInChannel);
        assertEquals(TimeUtil.Unit.SECONDS, query.getUnit());
        assertEquals("2015/05/07/11/05/02/000/", TimeUtil.millis(query.getStartTime()));

        query = generator.getQuery(latestStableInChannel);
        assertEquals(TimeUtil.Unit.SECONDS, query.getUnit());
        assertEquals("2015/05/07/11/05/03/000/", TimeUtil.millis(query.getStartTime()));
    }

    @Test
    void testMinuteOffsetTransition() {
        DateTime startTime = new DateTime(2015, 5, 7, 11, 30, 10, 55, DateTimeZone.UTC);
        DateTime latestStableInChannel = new DateTime(2015, 5, 7, 11, 33, 10, 851, DateTimeZone.UTC);
        QueryGenerator generator = new QueryGenerator(startTime, "test");

        TimeQuery query = generator.getQuery(latestStableInChannel);
        assertEquals(TimeUtil.Unit.MINUTES, query.getUnit());
        assertEquals("2015/05/07/11/30/10/055/", TimeUtil.millis(query.getStartTime()));

        query = generator.getQuery(latestStableInChannel);
        assertEquals(TimeUtil.Unit.MINUTES, query.getUnit());
        assertEquals("2015/05/07/11/31/00/000/", TimeUtil.millis(query.getStartTime()));

        query = generator.getQuery(latestStableInChannel);
        assertEquals(TimeUtil.Unit.SECONDS, query.getUnit());
        assertEquals("2015/05/07/11/32/00/000/", TimeUtil.millis(query.getStartTime()));

        query = generator.getQuery(latestStableInChannel);
        assertEquals(TimeUtil.Unit.SECONDS, query.getUnit());
        assertEquals("2015/05/07/11/32/01/000/", TimeUtil.millis(query.getStartTime()));

        query = generator.getQuery(latestStableInChannel);
        assertEquals(TimeUtil.Unit.SECONDS, query.getUnit());
        assertEquals("2015/05/07/11/32/02/000/", TimeUtil.millis(query.getStartTime()));
    }

    @Test
    void testHourOffsetTransition() {
        DateTime startTime = new DateTime(2015, 5, 7, 8, 30, 10, 55, DateTimeZone.UTC);
        DateTime latestStableInChannel = new DateTime(2015, 5, 7, 11, 33, 10, 851, DateTimeZone.UTC);
        QueryGenerator generator = new QueryGenerator(startTime, "test");

        TimeQuery query = generator.getQuery(latestStableInChannel);
        assertEquals(TimeUtil.Unit.HOURS, query.getUnit());
        assertEquals("2015/05/07/08/30/10/055/", TimeUtil.millis(query.getStartTime()));

        query = generator.getQuery(latestStableInChannel);
        assertEquals(TimeUtil.Unit.HOURS, query.getUnit());
        assertEquals("2015/05/07/09/00/00/000/", TimeUtil.millis(query.getStartTime()));

        query = generator.getQuery(latestStableInChannel);
        assertEquals(TimeUtil.Unit.MINUTES, query.getUnit());
        assertEquals("2015/05/07/10/00/00/000/", TimeUtil.millis(query.getStartTime()));

        query = generator.getQuery(latestStableInChannel);
        assertEquals(TimeUtil.Unit.MINUTES, query.getUnit());
        assertEquals("2015/05/07/10/01/00/000/", TimeUtil.millis(query.getStartTime()));

        query = generator.getQuery(latestStableInChannel);
        assertEquals(TimeUtil.Unit.MINUTES, query.getUnit());
        assertEquals("2015/05/07/10/02/00/000/", TimeUtil.millis(query.getStartTime()));
    }
}
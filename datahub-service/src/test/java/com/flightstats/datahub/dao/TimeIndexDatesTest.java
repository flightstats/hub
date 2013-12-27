package com.flightstats.datahub.dao;

import org.joda.time.DateTime;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 *
 */
public class TimeIndexDatesTest {

    @Test
    public void testHashstamp() throws Exception {
        DateTime dateTime = new DateTime(2013, 12, 26, 12, 59);
        String hashStamp = TimeIndexDates.getString(dateTime);
        assertEquals("2013-12-26T12:59-0800", hashStamp);
    }

    @Test
    public void testParse() throws Exception {
        DateTime dateTime = new DateTime(2013, 12, 26, 12, 59);
        DateTime parsed = TimeIndexDates.parse("2013-12-26T12:59-0800");
        assertEquals(dateTime, parsed);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNoMinutes() throws Exception {
        TimeIndexDates.parse("2013-12-26T12");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWrongFormat() throws Exception {
        TimeIndexDates.parse("2013/12-26T12:59");
    }

    @Test
    public void testDaylightSavingsTime() throws Exception {
        assertEquals("2014-11-02T00:30-0700", TimeIndexDates.getString(TimeIndexDates.parse("2014-11-02T00:30-0700")));
        assertEquals("2014-11-02T01:30-0700", TimeIndexDates.getString(TimeIndexDates.parse("2014-11-02T01:30-0700")));
        assertEquals("2014-11-02T01:30-0800", TimeIndexDates.getString(TimeIndexDates.parse("2014-11-02T02:30-0700")));
        assertEquals("2014-11-02T02:30-0800", TimeIndexDates.getString(TimeIndexDates.parse("2014-11-02T02:30-0800")));
    }

    @Test
    public void testTImeZones() throws Exception {
        assertEquals("2014-10-02T05:30-0700", TimeIndexDates.getString(TimeIndexDates.parse("2014-10-02T12:30-0000")));
    }
}

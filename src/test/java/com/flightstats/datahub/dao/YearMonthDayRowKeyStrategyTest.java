package com.flightstats.datahub.dao;

import com.flightstats.datahub.model.DataHubKey;
import org.junit.Test;

import java.util.Date;
import java.util.GregorianCalendar;

import static org.junit.Assert.assertEquals;

public class YearMonthDayRowKeyStrategyTest {

    @Test
    public void testBuildKey() throws Exception {
        DataHubKey key = new DataHubKey(new Date(12345678910L), (short) 1);

        YearMonthDayRowKeyStrategy testClass = new YearMonthDayRowKeyStrategy();

        String result = testClass.buildKey(null, key);

        assertEquals("19700523", result);
    }

    @Test
    public void testDateBoundard() throws Exception {
        GregorianCalendar calendar = new GregorianCalendar(2013, 1, 26, 16, 5); //4pm west coast is past midnight for the green witch
        DataHubKey key = new DataHubKey(calendar.getTime(), (short) 1);

        YearMonthDayRowKeyStrategy testClass = new YearMonthDayRowKeyStrategy();
        String result = testClass.buildKey("flib", key);

        assertEquals("20130227", result);
    }
}
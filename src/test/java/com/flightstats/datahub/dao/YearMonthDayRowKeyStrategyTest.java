package com.flightstats.datahub.dao;

import com.flightstats.datahub.model.DataHubKey;
import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.assertEquals;

public class YearMonthDayRowKeyStrategyTest {

    @Test
    public void testBuildKey() throws Exception {
        DataHubKey key = new DataHubKey(new Date(12345678910L), (short) 1);

        YearMonthDayRowKeyStrategy testClass = new YearMonthDayRowKeyStrategy();

        String result = testClass.buildKey(null, key);

        assertEquals("19700523", result);
    }
}
package com.flightstats.datahub.dao;

import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class YearMonthDayRowKeyStrategyTest {

    @Test
    public void testBuildKey() throws Exception {
        UUID uid = UUID.randomUUID();

        HectorFactoryWrapper hector = mock(HectorFactoryWrapper.class);

        when(hector.getTimeFromUUID(uid)).thenReturn(12345678910L);

        YearMonthDayRowKeyStrategy testClass = new YearMonthDayRowKeyStrategy(hector);
        String result = testClass.buildKey(null, uid);
        assertEquals("19700523", result);
    }
}
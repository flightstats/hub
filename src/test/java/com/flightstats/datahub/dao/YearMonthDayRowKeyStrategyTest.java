package com.flightstats.datahub.dao;

import com.flightstats.datahub.model.DataHubKey;
import org.joda.time.DateTime;
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

	@Test
	public void testNextKey() throws Exception {
		DateTime cal = new DateTime(2013, 7, 1, 0, 0);
		DataHubKey key = new DataHubKey(cal.toDate(), (short) 1);

		YearMonthDayRowKeyStrategy testClass = new YearMonthDayRowKeyStrategy();

		String result = testClass.nextKey(null, testClass.buildKey(null,key));

		assertEquals("20130702", result);
	}

	@Test
	public void testPrevKey() throws Exception {
		DateTime cal = new DateTime(2013, 1, 1, 0, 0);
		DataHubKey key = new DataHubKey(cal.toDate(), (short) 1);

		YearMonthDayRowKeyStrategy testClass = new YearMonthDayRowKeyStrategy();

		String result = testClass.prevKey(null, testClass.buildKey(null,key));

		assertEquals("20121231", result);
	}

	@Test
	public void testDateBoundard() throws Exception {
		DateTime dateTime = new DateTime(2013, 2, 26, 16, 5); //4pm west coast is past midnight for the green witch
		DataHubKey key = new DataHubKey(dateTime.toDate(), (short) 1);

		YearMonthDayRowKeyStrategy testClass = new YearMonthDayRowKeyStrategy();
		String result = testClass.buildKey("flib", key);

		assertEquals("20130227", result);
	}
}